package cz.muni.xmichalk.Traverser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.DTO.BundleSearchResultDTO;
import cz.muni.xmichalk.DTO.ConnectorDTO;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.DocumentValidity.StorageDocumentIntegrityVerifier;
import cz.muni.xmichalk.DocumentValidity.ValidityVerifier.ValidityVerifierRegistry;
import cz.muni.xmichalk.Models.*;
import cz.muni.xmichalk.ProvServiceAPI.ProvServiceAPI;
import cz.muni.xmichalk.ProvServiceTable.IProvServiceTable;
import cz.muni.xmichalk.SearchPriority.SearchPriorityComparatorResolver;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Traverser {
    private final IProvServiceTable provServiceTable;
    private final ValidityVerifierRegistry validityVerifierRegistry;
    private static final Logger log = LoggerFactory.getLogger(Traverser.class);
    private final int concurrencyDegree;

    public Traverser(IProvServiceTable traverserTable, int concurrencyDegree, ValidityVerifierRegistry validityVerifierRegistry) {
        this.provServiceTable = traverserTable;
        this.concurrencyDegree = concurrencyDegree;
        this.validityVerifierRegistry = validityVerifierRegistry;
        log.info("Instantiated traverser with concurrency degree: {}", concurrencyDegree);
    }


    /***
     * Traverses the chain in given direction, searching each bundle for given target. Returns all found results matching the target.
     * @param startBundleId - identifier of the bundle
     * @param startNodeId - identifier of the starting node in the bundle
     * @param searchParams - direction of search and characterization of the target we are searching for
     * @return - list of found results matching the target
     */
    public List<FoundResult> searchChain(QualifiedName startBundleId, QualifiedName startNodeId, SearchParams searchParams) {
        Comparator<ItemToSearch> searchPriorityComparator = SearchPriorityComparatorResolver.getSearchPriorityComparator(searchParams);

        SearchState searchState = new SearchState(
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new PriorityBlockingQueue<>(10, searchPriorityComparator),
                ConcurrentHashMap.newKeySet()
        );

        searchState.toSearchQueue.add(new ItemToSearch(
                startBundleId,
                startNodeId,
                provServiceTable.getServiceUri(startBundleId.getUri()),
                true,
                searchParams.validityChecks.stream().collect(Collectors.toMap(
                        check -> check, check -> true)
                )));

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyDegree);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicInteger runningTasks = new AtomicInteger(0);

        for (int i = 0; i < concurrencyDegree; i++) {
            ItemToSearch itemToSearch = pollNextToSearch(searchState, searchParams.versionPreference);
            if (itemToSearch != null) {
                submitSearchTask(itemToSearch, searchState, searchParams,
                        completionService, runningTasks);
            }
        }

        try {
            while (runningTasks.get() > 0) {
                completionService.take(); // wait for a task to finish
                runningTasks.decrementAndGet();

                ItemToSearch next;
                while (runningTasks.get() < concurrencyDegree
                        && (next = pollNextToSearch(searchState, searchParams.versionPreference)) != null) {
                    submitSearchTask(next, searchState, searchParams,
                            completionService, runningTasks);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        return List.copyOf(searchState.results);
    }

    private ItemToSearch pollNextToSearch(SearchState searchState, String versionPreference) {
        log.info("Poll next item to search");
        ItemToSearch itemToSearch;
        while ((itemToSearch = searchState.toSearchQueue.poll()) != null) {
            final ItemToSearch finalItemToSearch = itemToSearch;
            if (searchState.processing.values().stream()
                    .anyMatch(item -> searchState.toSearchQueue.comparator().compare(item, finalItemToSearch) < 0)) {
                return null; // wait until all bundles with higher priority are processed, because they might add higher priority items to search
            }

            try {
                QualifiedName preferredVersion = ProvServiceAPI.fetchPreferredBundleVersion(itemToSearch.provServiceUri, itemToSearch.bundleId, versionPreference);
                if (preferredVersion != null) {
                    log.info("Fetch preferred version for bundle: {} returned {}", itemToSearch.bundleId.getUri(), preferredVersion.getUri());
                    itemToSearch.bundleId = preferredVersion;
                } else {
                    log.warn("Fetch preferred version for bundle: {} returned null", itemToSearch.bundleId.getUri());
                }
            } catch (Exception e) {
                log.error("Error while fetching preferred version for bundle {}: {}", itemToSearch.bundleId.getUri(), e);
            }

            if (searchState.processing.putIfAbsent(itemToSearch.bundleId, itemToSearch) == null) {
                if (!searchState.visited.containsKey(itemToSearch.bundleId)) {
                    log.info("Next to search bundle id: {}, connector id: {}, pathIntegrity: {}, pathValidityChecks: {}",
                            itemToSearch.bundleId.getUri(), itemToSearch.connectorId.getUri(), itemToSearch.pathIntegrity, itemToSearch.pathValidityChecks);
                    return itemToSearch;
                } else {
                    searchState.processing.remove(itemToSearch.bundleId, itemToSearch);
                    log.info("Already searched bundle: {}", itemToSearch.bundleId.getUri());
                }
            } else {
                log.info("Already searching bundle: {}", itemToSearch.bundleId.getUri());
            }


        }
        return null;
    }

    private void submitSearchTask(ItemToSearch itemToSearch,
                                  SearchState searchState,
                                  SearchParams searchParams,
                                  CompletionService<Void> completionService,
                                  AtomicInteger runningTasks) {
        runningTasks.incrementAndGet();
        completionService.submit(() -> {
            log.info("Started processing bundle {} from connector {}", itemToSearch.bundleId.getUri(), itemToSearch.connectorId.getUri());

            boolean hasIntegrity = false;
            Map<EValiditySpecification, Boolean> validityChecks = searchParams.validityChecks.stream().collect(Collectors.toMap(
                    check -> check, check -> false));

            try {
                var findTargetResult = ProvServiceAPI.fetchSearchBundleResult(itemToSearch.provServiceUri, itemToSearch.bundleId, itemToSearch.connectorId,
                        searchParams.targetType, searchParams.targetSpecification);

                var findConnectorsResult = ProvServiceAPI.fetchSearchBundleResult(
                        itemToSearch.provServiceUri, itemToSearch.bundleId, itemToSearch.connectorId, "connectors",
                        new ObjectMapper().valueToTree(searchParams.searchBackwards ? "backward" : "forward"));

                hasIntegrity = hasIntegrity(itemToSearch.bundleId, findTargetResult, findConnectorsResult);

                for (var validityCheck : searchParams.validityChecks) {
                    boolean validityResult = validityVerifierRegistry.verifyValidity(
                            itemToSearch, findTargetResult, validityCheck);
                    validityChecks.put(validityCheck, validityResult);
                }

                var newResult = convertToNewResult(itemToSearch, findTargetResult, hasIntegrity, validityChecks);
                if (newResult != null) {
                    searchState.results.add(newResult);
                    log.info("In bundle {} found target(s): {}", itemToSearch.bundleId.getUri(), newResult.result.toString());
                }

                var newItemsToSearch = convertToNewItemsToSearch(itemToSearch, findConnectorsResult, hasIntegrity, validityChecks);
                searchState.toSearchQueue.addAll(newItemsToSearch);
                log.info("In bundle {} found connections to: {}", itemToSearch.bundleId.getUri(),
                        newItemsToSearch.stream().map(item -> item.bundleId.getUri())
                                .collect(Collectors.joining(", ")));


            } catch (Exception e) {
                log.error("Error while processing bundle {}: {}", itemToSearch.bundleId.getUri(), e);
            } finally {
                searchState.visited.put(itemToSearch.bundleId, new VisitedItem(itemToSearch.bundleId));
                searchState.processing.remove(itemToSearch.bundleId, itemToSearch);

            }

            log.info("Finished processing bundle: {}", itemToSearch.bundleId.getUri());

            return null;
        });
    }

    private boolean hasIntegrity(QualifiedName bundleId, BundleSearchResultDTO targetSearchResult, BundleSearchResultDTO connectorsSearchResult) {
        if (targetSearchResult == null && connectorsSearchResult == null) {
            return false;
        }
        if (targetSearchResult == null) {
            return StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, connectorsSearchResult.token());
        }
        if (connectorsSearchResult == null) {
            return StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, targetSearchResult.token());
        }
        return targetSearchResult.token().equals(connectorsSearchResult.token())
                && StorageDocumentIntegrityVerifier.verifyIntegrity(bundleId, targetSearchResult.token());
    }

    private FoundResult convertToNewResult(ItemToSearch itemSearched, BundleSearchResultDTO findTargetResult, boolean integrity, Map<EValiditySpecification, Boolean> validityChecks) {
        if (findTargetResult == null || findTargetResult.found() == null || findTargetResult.found().isNull()) {
            log.warn("Search bundle {} for target returned null", itemSearched.bundleId.getUri());
            return null;
        }

        return new FoundResult(
                itemSearched.bundleId,
                findTargetResult.found(),
                integrity,
                validityChecks,
                itemSearched.pathIntegrity,
                itemSearched.pathValidityChecks);
    }

    private List<ItemToSearch> convertToNewItemsToSearch(ItemToSearch itemSearched, BundleSearchResultDTO findConnectorsResult, boolean integrity, Map<EValiditySpecification, Boolean> validityChecks) {
        if (findConnectorsResult == null || findConnectorsResult.found() == null || findConnectorsResult.found().isNull()) {
            log.warn("Search bundle {} for connectors returned null", itemSearched.bundleId.getUri());
            return new ArrayList<>();
        }

        List<ConnectorDTO> connectors;
        try {
            connectors = new ObjectMapper().convertValue(
                    findConnectorsResult.found(), new TypeReference<List<ConnectorDTO>>() {
                    });
        } catch (Exception e) {
            log.error("While converting connectors from bundle {} got error: {}", itemSearched.bundleId.getUri(), e);
            return new ArrayList<>();
        }

        List<ItemToSearch> newItemsToSearch = new ArrayList<>();

        for (ConnectorDTO connector : connectors) {
            if (connector == null || connector.referencedBundleId == null) continue;

            String provServiceUri = connector.provenanceServiceUri;
            if (provServiceUri == null) {
                provServiceUri = provServiceTable.getServiceUri(connector.referencedBundleId.toDomainModel().getUri());
            }

            newItemsToSearch.add(
                    new ItemToSearch(
                            connector.referencedBundleId.toDomainModel(),
                            connector.referencedConnectorId.toDomainModel(),
                            provServiceUri,
                            itemSearched.pathIntegrity && integrity,
                            combineValidityChecks(itemSearched.pathValidityChecks, validityChecks)
                    )
            );
        }

        return newItemsToSearch;
    }

    private Map<EValiditySpecification, Boolean> combineValidityChecks(Map<EValiditySpecification, Boolean> first, Map<EValiditySpecification, Boolean> second) {
        Map<EValiditySpecification, Boolean> combined = new HashMap<>();
        for (EValiditySpecification key : first.keySet()) {
            boolean firstValue = first.getOrDefault(key, false);
            boolean secondValue = second.getOrDefault(key, false);
            combined.put(key, firstValue && secondValue);
        }
        return combined;
    }
}
