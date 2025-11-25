package cz.muni.xmichalk.traverser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.dto.ConnectorDTO;
import cz.muni.xmichalk.integrity.StorageDocumentIntegrityVerifier;
import cz.muni.xmichalk.models.*;
import cz.muni.xmichalk.provServiceAPI.ProvServiceAPI;
import cz.muni.xmichalk.provServiceTable.IProvServiceTable;
import cz.muni.xmichalk.traversalPriority.ETraversalPriority;
import cz.muni.xmichalk.traversalPriority.UnsupportedTraversalPriorityException;
import cz.muni.xmichalk.validity.EValidityCheck;
import cz.muni.xmichalk.validity.IValidityVerifier;
import cz.muni.xmichalk.validity.UnsupportedValidityCheckException;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Traverser {
    private final IProvServiceTable provServiceTable;
    private final Map<EValidityCheck, IValidityVerifier> validityVerifiers;
    private final Map<ETraversalPriority, Comparator<ItemToTraverse>> traversalPriorityComparators;
    private static final Logger log = LoggerFactory.getLogger(Traverser.class);
    private final int concurrencyDegree;

    public Traverser(
            IProvServiceTable traverserTable,
            int concurrencyDegree,
            Map<EValidityCheck, IValidityVerifier> validityCheckers,
            Map<ETraversalPriority, Comparator<ItemToTraverse>> traversalPriorityComparators) {
        this.provServiceTable = traverserTable;
        this.concurrencyDegree = concurrencyDegree;
        this.validityVerifiers = validityCheckers;
        this.traversalPriorityComparators = traversalPriorityComparators;
        log.info("Instantiated traverser with concurrency degree: {}", concurrencyDegree);
    }

    public Map<EValidityCheck, IValidityVerifier> getValidityVerifiers() {
        return validityVerifiers;
    }

    public Map<ETraversalPriority, Comparator<ItemToTraverse>> getTraversalPriorityComparators() {
        return traversalPriorityComparators;
    }

    public TraversalResults traverseChain(QualifiedName startBundleId, QualifiedName startNodeId, TraversalParams traversalParams) {
        Comparator<ItemToTraverse> traversalPriorityComparator = traversalPriorityComparators.get(traversalParams.traversalPriority);
        if (traversalPriorityComparator == null) {
            String errorMessage = "No traversal priority comparator registered for: " + traversalParams.traversalPriority;
            log.error(errorMessage);
            throw new UnsupportedTraversalPriorityException(errorMessage);
        }

        TraversalState traversalState = new TraversalState(
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new PriorityBlockingQueue<>(10, traversalPriorityComparator),
                new ConcurrentLinkedQueue<>(),
                new ConcurrentLinkedQueue<>()
        );

        traversalState.toTraverseQueue.add(
                new ItemToTraverse(
                        startBundleId,
                        startNodeId,
                        provServiceTable.getServiceUri(startBundleId.getUri()),
                        true,
                        new LinkedHashMap<>(traversalParams.validityChecks.stream().collect(Collectors.toMap(
                                check -> check, check -> true))
                        )
                )
        );

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyDegree);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicInteger runningTasks = new AtomicInteger(0);

        submitTraverseTasks(traversalState, traversalParams, completionService, runningTasks);

        try {
            while (runningTasks.get() > 0) {
                completionService.take(); // wait for a task to finish
                runningTasks.decrementAndGet();

                submitTraverseTasks(traversalState, traversalParams, completionService, runningTasks);
            }
        } catch (InterruptedException e) {
            log.warn(e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        return new TraversalResults(traversalState.results.stream().toList(), traversalState.errors);
    }

    private void submitTraverseTasks(
            TraversalState traversalState,
            TraversalParams traversalParams,
            CompletionService<Void> completionService,
            AtomicInteger runningTasks
    ) {
        ItemToTraverse next;
        while (runningTasks.get() < concurrencyDegree
                && (next = pollNextToTraverse(traversalState, traversalParams.versionPreference)) != null) {
            runningTasks.incrementAndGet();
            final ItemToTraverse finalNext = next;
            completionService.submit(() -> {
                traverseItem(finalNext, traversalState, traversalParams);
                return null;
            });
        }
    }

    private void traverseItem(ItemToTraverse itemToTraverse,
                              TraversalState traversalState,
                              TraversalParams traversalParams) {
        log.info("Started processing bundle {} from connector {}", itemToTraverse.bundleId.getUri(), itemToTraverse.connectorId.getUri());

        try {
            BundleQueryResultDTO queryResult = ProvServiceAPI.fetchBundleQueryResult(
                    itemToTraverse.provServiceUri, itemToTraverse.bundleId, itemToTraverse.connectorId, traversalParams.querySpecification);

            BundleQueryResultDTO findConnectorsResult = ProvServiceAPI.fetchBundleConnectors(
                    itemToTraverse.provServiceUri, itemToTraverse.bundleId, itemToTraverse.connectorId,
                    traversalParams.traverseBackwards);

            boolean hasIntegrity = hasIntegrity(itemToTraverse.bundleId, queryResult, findConnectorsResult);

            LinkedHashMap<EValidityCheck, Boolean> validityChecks = evaluateValidityChecks(
                    traversalParams.validityChecks, itemToTraverse, queryResult);

            ResultFromBundle newResult = convertToNewResult(itemToTraverse, queryResult, hasIntegrity, validityChecks);
            if (newResult != null) {
                traversalState.results.add(newResult);
                log.info("In bundle {} found query result: {}", itemToTraverse.bundleId.getUri(), newResult.result.toString());
            }

            List<ItemToTraverse> newItemsToTraverse = convertToNewItemsToTraverse(itemToTraverse, findConnectorsResult, hasIntegrity, validityChecks);
            traversalState.toTraverseQueue.addAll(newItemsToTraverse);
            log.info("In bundle {} found connections to: {}", itemToTraverse.bundleId.getUri(),
                    newItemsToTraverse.stream().map(item -> item.bundleId.getUri())
                            .collect(Collectors.joining(", ")));


        } catch (Exception e) {
            String errorMessage = "Error while processing bundle: " + itemToTraverse.bundleId.getUri() + ", error: " + e.getMessage();
            log.error(errorMessage);
            traversalState.errors.add(errorMessage);
        } finally {
            traversalState.visited.put(itemToTraverse.bundleId, new VisitedItem(itemToTraverse.bundleId));
            traversalState.processing.remove(itemToTraverse.bundleId, itemToTraverse);
            log.info("Finished processing bundle: {}", itemToTraverse.bundleId.getUri());
        }
    }

    private ItemToTraverse pollNextToTraverse(TraversalState traversalState, String versionPreference) {
        ItemToTraverse itemToTraverse;
        while ((itemToTraverse = traversalState.toTraverseQueue.poll()) != null) {
            final ItemToTraverse finalItemToTraverse = itemToTraverse;
            if (traversalState.processing.values().stream()
                    .anyMatch(item -> traversalState.toTraverseQueue.comparator().compare(item, finalItemToTraverse) < 0)) {
                traversalState.toTraverseQueue.add(finalItemToTraverse);
                return null; // wait until all bundles with higher priority are processed, because they might add higher priority items to traverse
            }

            try {
                QualifiedName preferredVersion = ProvServiceAPI.fetchPreferredBundleVersion(itemToTraverse.provServiceUri, itemToTraverse.bundleId, itemToTraverse.connectorId, versionPreference);
                if (preferredVersion != null) {
                    log.info("Fetch preferred version for bundle: {} returned {}", itemToTraverse.bundleId.getUri(), preferredVersion.getUri());
                    itemToTraverse.bundleId = preferredVersion;
                } else {
                    log.warn("Fetch preferred version for bundle: {} returned null", itemToTraverse.bundleId.getUri());
                }
            } catch (Exception e) {
                log.error("Error while fetching preferred version for bundle {}: {}", itemToTraverse.bundleId.getUri(), e);
            }

            if (traversalState.processing.putIfAbsent(itemToTraverse.bundleId, itemToTraverse) == null) {
                if (!traversalState.visited.containsKey(itemToTraverse.bundleId)) {
                    return itemToTraverse;
                } else {
                    traversalState.processing.remove(itemToTraverse.bundleId, itemToTraverse);
                    log.info("Already traversed bundle: {}", itemToTraverse.bundleId.getUri());
                }
            } else {
                log.info("Already traversing bundle: {}", itemToTraverse.bundleId.getUri());
            }


        }
        return null;
    }

    private boolean hasIntegrity(QualifiedName bundleId, BundleQueryResultDTO queryResult, BundleQueryResultDTO findConnectorsResult) {
        if (queryResult == null && findConnectorsResult == null) {
            return false;
        }
        if (queryResult == null) {
            return StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, findConnectorsResult.token);
        }
        if (findConnectorsResult == null) {
            return StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, queryResult.token);
        }
        return queryResult.token.equals(findConnectorsResult.token)
                && StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, queryResult.token);
    }

    private LinkedHashMap<EValidityCheck, Boolean> evaluateValidityChecks(List<EValidityCheck> validityChecks, ItemToTraverse itemTraversed, BundleQueryResultDTO queryResult) {
        LinkedHashMap<EValidityCheck, Boolean> validityCheckValues = new LinkedHashMap<>();
        for (EValidityCheck validityCheck : validityChecks) {
            IValidityVerifier verifier = validityVerifiers.get(validityCheck);
            if (verifier != null) {
                boolean result = verifier.verify(itemTraversed, queryResult);
                validityCheckValues.put(validityCheck, result);
            } else {
                String errorMessage = "No validity checker registered for: " + validityCheck;
                log.error(errorMessage);
                throw new UnsupportedValidityCheckException(errorMessage);
            }
        }
        return validityCheckValues;
    }

    private ResultFromBundle convertToNewResult(ItemToTraverse itemTraversed, BundleQueryResultDTO queryResult, boolean integrity, Map<EValidityCheck, Boolean> validityChecks) {
        if (queryResult == null || queryResult.result == null || queryResult.result.isNull()) {
            log.warn("Query result for bundle {} is null", itemTraversed.bundleId.getUri());
            return null;
        }

        return new ResultFromBundle(
                itemTraversed.bundleId,
                queryResult.result,
                integrity,
                validityChecks,
                itemTraversed.pathIntegrity,
                itemTraversed.pathValidityChecks);
    }

    private List<ItemToTraverse> convertToNewItemsToTraverse(ItemToTraverse itemTraversed, BundleQueryResultDTO findConnectorsResult, boolean integrity, LinkedHashMap<EValidityCheck, Boolean> validityChecks) {
        if (findConnectorsResult == null || findConnectorsResult.result == null || findConnectorsResult.result.isNull()) {
            log.warn("Find connectors in bundle {} returned null", itemTraversed.bundleId.getUri());
            return new ArrayList<>();
        }

        List<ConnectorDTO> connectors;
        try {
            connectors = new ObjectMapper().convertValue(
                    findConnectorsResult.result, new TypeReference<List<ConnectorDTO>>() {
                    });
        } catch (Exception e) {
            log.error("While converting connectors from bundle {} got error: {}", itemTraversed.bundleId.getUri(), e);
            return new ArrayList<>();
        }

        List<ItemToTraverse> newItemsToTraverse = new ArrayList<>();

        for (ConnectorDTO connector : connectors) {
            if (connector == null || connector.referencedBundleId == null) continue;

            String provServiceUri = connector.provenanceServiceUri;
            if (provServiceUri == null) {
                provServiceUri = provServiceTable.getServiceUri(connector.referencedBundleId.toQN().getUri());
            }

            newItemsToTraverse.add(
                    new ItemToTraverse(
                            connector.referencedBundleId.toQN(),
                            connector.referencedConnectorId.toQN(),
                            provServiceUri,
                            itemTraversed.pathIntegrity && integrity,
                            combineValidityChecks(itemTraversed.pathValidityChecks, validityChecks)
                    )
            );
        }

        return newItemsToTraverse;
    }

    private LinkedHashMap<EValidityCheck, Boolean> combineValidityChecks(LinkedHashMap<EValidityCheck, Boolean> first, LinkedHashMap<EValidityCheck, Boolean> second) {
        LinkedHashMap<EValidityCheck, Boolean> combined = new LinkedHashMap<>();
        for (EValidityCheck key : first.sequencedKeySet()) {
            boolean firstValue = first.getOrDefault(key, false);
            boolean secondValue = second.getOrDefault(key, false);
            combined.put(key, firstValue && secondValue);
        }
        return combined;
    }
}
