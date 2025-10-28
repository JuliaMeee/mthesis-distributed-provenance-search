package cz.muni.xmichalk.Traverser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.DTO.*;
import cz.muni.xmichalk.DocumentValidity.StorageDocumentIntegrityVerifier;
import cz.muni.xmichalk.Models.*;
import cz.muni.xmichalk.ProvServiceTable.IProvServiceTable;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Traverser {
    private final IProvServiceTable provServiceTable;
    private static final Logger log = LoggerFactory.getLogger(Traverser.class);
    private final int concurrencyDegree = 10;

    private static final Comparator<ItemToSearch> toSearchPriorityComparator =
            Comparator.comparing(e -> !e.hasPathIntegrity);

    public Traverser(IProvServiceTable traverserTable) {
        this.provServiceTable = traverserTable;
    }


    /***
     * Traverses the chain in given direction, searching each bundle for given target. Returns all found results matching the target.
     * @param startBundleId - identifier of the bundle
     * @param startNodeId - identifier of the starting node in the bundle
     * @param searchParams - direction of search and characterization of the target we are searching for
     * @return - list of found results matching the target
     */
    public List<FoundResult> searchChain(QualifiedName startBundleId, QualifiedName startNodeId, SearchParams searchParams) {
        SearchState searchState = new SearchState(
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new PriorityBlockingQueue<>(10, toSearchPriorityComparator),
                ConcurrentHashMap.newKeySet()
        );

        searchState.toSearch.add(new ItemToSearch(startBundleId, startNodeId, true, true));

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
        ItemToSearch itemToSearch;
        while ((itemToSearch = searchState.toSearch.poll()) != null) {
            if (!itemToSearch.hasPathIntegrity && searchState.processing.values().stream()
                    .anyMatch(item -> item.hasPathIntegrity)) {
                return null; // wait until all valid  bundles are processed before continuing with lower credibility ones
            }

            var preferredVersion = fetchPreferredBundleVersion(itemToSearch.bundleId, versionPreference);
            if (preferredVersion != null) {

                log.info("Fetch preferred version for bundle: {} returned {}", itemToSearch.bundleId.getUri(), preferredVersion.getUri());
                itemToSearch.bundleId = preferredVersion;
            } else {
                log.warn("Fetch preferred version for bundle: {} returned null", itemToSearch.bundleId.getUri());
            }

            if (searchState.processing.putIfAbsent(itemToSearch.bundleId, itemToSearch) == null) {
                if (!searchState.visited.containsKey(itemToSearch.bundleId)) {
                    return itemToSearch;
                } else {
                    searchState.processing.remove(itemToSearch.bundleId, itemToSearch);
                }
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

            try {
                var findTargetResult = fetchSearchBundleResult(itemToSearch.bundleId, itemToSearch.connectorId,
                        searchParams.targetType, searchParams.targetSpecification);

                var findConnectorsResult = fetchSearchBundleResult(
                        itemToSearch.bundleId, itemToSearch.connectorId, "connectors",
                        new ObjectMapper().valueToTree(searchParams.searchBackwards ? "backward" : "forward"));

                boolean hasIntegrity = hasIntegrity(findTargetResult, findConnectorsResult);
                boolean isValid = false;// TODO validity

                var newResult = convertToNewResult(itemToSearch, findTargetResult, hasIntegrity, isValid);
                if (newResult != null) {
                    searchState.results.add(newResult);
                    log.info("In bundle {} found target(s): {}", itemToSearch.bundleId.getUri(), newResult.result.toString());
                }

                var newItemsToSearch = convertToNewItemsToSearch(itemToSearch, findConnectorsResult, hasIntegrity, isValid);
                searchState.toSearch.addAll(newItemsToSearch);
                log.info("In bundle {} found connections to: {}", itemToSearch.bundleId.getUri(),
                        newItemsToSearch.stream().map(item -> item.bundleId.getUri())
                                .collect(Collectors.joining(", ")));


            } catch (Exception e) {
                log.error("Error while processing bundle {}: {}", itemToSearch.bundleId.getUri(), e.getMessage());
            } finally {
                searchState.visited.put(itemToSearch.bundleId, new VisitedItem(itemToSearch.bundleId));
                searchState.processing.remove(itemToSearch.bundleId, itemToSearch);

            }

            log.info("Finished processing bundle: {}", itemToSearch.bundleId.getUri());

            return null;
        });
    }

    private boolean hasIntegrity(BundleSearchResultDTO targetSearchResult, BundleSearchResultDTO connectorsSearchResult) {
        if (targetSearchResult == null && connectorsSearchResult == null) {
            return false;
        }
        if (targetSearchResult == null) {
            return StorageDocumentIntegrityVerifier.verifySignature(connectorsSearchResult.token());
        }
        if (connectorsSearchResult == null) {
            return StorageDocumentIntegrityVerifier.verifySignature(targetSearchResult.token());
        }
        return targetSearchResult.token().equals(connectorsSearchResult.token())
                && StorageDocumentIntegrityVerifier.verifySignature(targetSearchResult.token());
    }

    private FoundResult convertToNewResult(ItemToSearch itemSearched, BundleSearchResultDTO findTargetResult, boolean hasIntegrity, boolean isValid) {
        if (findTargetResult == null || findTargetResult.found() == null || findTargetResult.found().isNull()) {
            log.warn("Search bundle {} for target returned null", itemSearched.bundleId.getUri());
            return null;
        }

        return new FoundResult(itemSearched.bundleId, findTargetResult.found(), itemSearched.hasPathIntegrity, hasIntegrity, itemSearched.isPathValid, isValid);
    }

    private List<ItemToSearch> convertToNewItemsToSearch(ItemToSearch itemSearched, BundleSearchResultDTO findConnectorsResult, boolean hasIntegrity, boolean isValid) {
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
            log.error("While converting connectors from bundle {} got error: {}", itemSearched.bundleId.getUri(), e.getMessage());
            return new ArrayList<>();
        }

        List<ItemToSearch> newItemsToSearch = new ArrayList<>();

        for (ConnectorDTO connector : connectors) {
            if (connector == null || connector.referencedBundleId == null) continue;

            newItemsToSearch.add(
                    new ItemToSearch(
                            connector.referencedBundleId.toDomainModel(),
                            connector.id.toDomainModel(),
                            itemSearched.hasPathIntegrity && hasIntegrity,
                            itemSearched.isPathValid && isValid
                    )
            );
        }

        return newItemsToSearch;
    }

    public BundleSearchResultDTO fetchSearchBundleResult(
            QualifiedName bundleId, QualifiedName connectorId,
            String targetType, JsonNode targetSpecification) throws IOException {
        BundleSearchParamsDTO searchParams = new BundleSearchParamsDTO(bundleId, connectorId, targetType, targetSpecification);
        String ServiceUri = provServiceTable.getServiceUri(bundleId.getUri());
        if (ServiceUri == null) {
            throw new IOException("No prov service found for bundle: " + bundleId.getUri());
        }

        String url = ServiceUri + "/api/searchBundle";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BundleSearchParamsDTO> request = new HttpEntity<>(searchParams, headers);

        ResponseEntity<BundleSearchResultDTO> response = restTemplate.postForEntity(
                url, request, BundleSearchResultDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Search bundle API call failed with status: " + response.getStatusCode());
        }

        BundleSearchResultDTO searchBundleResultDTO = response.getBody();

        return searchBundleResultDTO;
    }

    public QualifiedName fetchPreferredBundleVersion(QualifiedName bundleId, String versionpreference) {
        PickVersionParamsDTO params = new PickVersionParamsDTO(
                new QualifiedNameDTO().from(bundleId),
                versionpreference);

        String serviceUri = provServiceTable.getServiceUri(bundleId.getUri());
        if (serviceUri == null) {
            log.error("No prov service found for bundle: {}", bundleId.getUri());
            return null;
        }

        String url = serviceUri + "/api/pickVersion";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PickVersionParamsDTO> request = new HttpEntity<>(params, headers);

        ResponseEntity<QualifiedNameDTO> response = restTemplate.postForEntity(
                url, request, QualifiedNameDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Get preferred version API call failed with status: {}", response.getStatusCode());
            return null;
        }

        if (response.getBody() == null) {
            return null;
        }

        return response.getBody().toDomainModel();
    }

    private ECredibility mergeCredibility(ECredibility bundleCredibility, ECredibility pathCredibility) {
        if (bundleCredibility == ECredibility.INVALID) return ECredibility.INVALID;
        if (bundleCredibility == ECredibility.VALID && pathCredibility == ECredibility.VALID) return ECredibility.VALID;
        return ECredibility.LOW;
    }
}
