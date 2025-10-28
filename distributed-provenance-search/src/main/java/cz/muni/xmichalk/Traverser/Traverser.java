package cz.muni.xmichalk.Traverser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.DTO.*;
import cz.muni.xmichalk.DocumentValidity.StorageDocumentIntegrityVerifier;
import cz.muni.xmichalk.DocumentValidity.StorageDocumentValidityVerifier;
import cz.muni.xmichalk.Models.*;
import cz.muni.xmichalk.ProvServiceTable.IProvServiceTable;
import org.openprovenance.prov.model.QualifiedName;
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

public class Traverser {
    private final IProvServiceTable provServiceTable;
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
            ItemToSearch itemToSearch = pollNextToSearch(searchState);
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
                        && (next = pollNextToSearch(searchState)) != null) {
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

    private ItemToSearch pollNextToSearch(SearchState searchState) {
        ItemToSearch itemToSearch;
        while ((itemToSearch = searchState.toSearch.poll()) != null) {
            if (!itemToSearch.hasPathIntegrity && searchState.processing.values().stream()
                    .anyMatch(item -> item.hasPathIntegrity)) {
                return null; // wait until all valid  bundles are processed before continuing with lower credibility ones
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
            System.out.println("Processing bundle: " + itemToSearch.bundleId.getLocalPart());

            try {
                var newResult = getNewResult(itemToSearch, searchParams);
                if (newResult != null) {
                    searchState.results.add(newResult);
                    System.out.println("Found result in bundle: " + itemToSearch.bundleId.getLocalPart());
                }

                var newItemsToSearch = getNewItemsToSearch(itemToSearch, searchParams);
                searchState.toSearch.addAll(newItemsToSearch);
                System.out.println("Found " + newItemsToSearch.size() + " items to search in bundle: " + itemToSearch.bundleId.getLocalPart());

                // TODO validity

            } catch (Exception e) {
                System.err.println("Error during search bundle " + itemToSearch.bundleId.getLocalPart() + ": " + e.getMessage());
            } finally {
                searchState.visited.put(itemToSearch.bundleId, new VisitedItem(itemToSearch.bundleId));
                searchState.processing.remove(itemToSearch.bundleId, itemToSearch);

            }

            System.out.println("Finished processing bundle: " + itemToSearch.bundleId.getLocalPart());

            return null;
        });
    }

    private FoundResult getNewResult(ItemToSearch itemToSearch, SearchParams searchParams) throws IOException {
        var searchBundleResult = fetchSearchBundleResult(itemToSearch.bundleId, itemToSearch.connectorId,
                searchParams.targetType, searchParams.targetSpecification);
        if (searchBundleResult == null || searchBundleResult.found == null || searchBundleResult.found.isNull()) {
            return null;
        }

        boolean hasIntegrity = StorageDocumentIntegrityVerifier.verifySignature(searchBundleResult.token);
        boolean isValid = new StorageDocumentValidityVerifier().verifyValidity(searchBundleResult.token);

        return new FoundResult(searchBundleResult.bundleId.toDomainModel(), searchBundleResult.found, itemToSearch.hasPathIntegrity, hasIntegrity, itemToSearch.isPathValid, isValid);
    }

    private List<ItemToSearch> getNewItemsToSearch(ItemToSearch itemToSearch, SearchParams searchParams) throws IOException {
        var searchBundleResult = fetchSearchBundleResult(
                itemToSearch.bundleId, itemToSearch.connectorId, "connectors",
                new ObjectMapper().valueToTree(searchParams.searchBackwards ? "backward" : "forward"));

        if (searchBundleResult == null || searchBundleResult.found == null || searchBundleResult.found.isNull()) {
            return new ArrayList<>();
        }

        List<ConnectorDTO> connectors;

        try {
            connectors = new ObjectMapper().convertValue(
                    searchBundleResult.found, new TypeReference<List<ConnectorDTO>>() {
                    }
            );
        } catch (Exception e) {
            System.out.println("Error converting found connectors: " + e.getMessage());
            return new ArrayList<>();
        }

        boolean hasPathIntegrity = itemToSearch.hasPathIntegrity && StorageDocumentIntegrityVerifier.verifySignature(searchBundleResult.token);
        boolean isPathValid = itemToSearch.isPathValid && new StorageDocumentValidityVerifier().verifyValidity(searchBundleResult.token);

        return convertToNewItemsToSearch(connectors, searchParams, hasPathIntegrity, isPathValid);
    }

    public List<ItemToSearch> convertToNewItemsToSearch(List<ConnectorDTO> connectors, SearchParams searchParams, boolean hasPathIntegrity, boolean isPathValid) throws IOException {
        List<ItemToSearch> newItemsToSearch = new ArrayList<>();

        for (ConnectorDTO connector : connectors) {
            if (connector == null || connector.referencedBundleId == null) continue;

            var referencedBundleId = connector.referencedBundleId.toDomainModel();
            var preferredVersion = fetchPreferredBundleVersion(referencedBundleId, searchParams.versionPreference);

            if (preferredVersion == null) {
                continue;
            }

            newItemsToSearch.add(
                    new ItemToSearch(
                            preferredVersion.toDomainModel(),
                            connector.id.toDomainModel(),
                            hasPathIntegrity,
                            isPathValid
                    )
            );
        }

        return newItemsToSearch;
    }

    public BundleSearchResponseDTO fetchSearchBundleResult(
            QualifiedName bundleId, QualifiedName connectorId,
            String targetType, JsonNode targetSpecification) throws IOException {
        BundleSearchParamsDTO searchParams = new BundleSearchParamsDTO(bundleId, connectorId, targetType, targetSpecification);
        String ServiceUri = provServiceTable.getServiceUri(bundleId.getUri());
        if (ServiceUri == null) {
            throw new IOException("No prov service found for bundle: " + bundleId.getUri());
        }

        String url = ServiceUri + "/api/searchBundle";

        System.out.println("Fetching results for bundle " + bundleId.getUri() + " via: " + url + " connector: " + connectorId.getUri() + " targetType: " + targetType + " targetSpec: " + targetSpecification.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BundleSearchParamsDTO> request = new HttpEntity<>(searchParams, headers);

        ResponseEntity<BundleSearchResponseDTO> response = restTemplate.postForEntity(
                url, request, BundleSearchResponseDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("API call failed with status: " + response.getStatusCode());
        }

        BundleSearchResponseDTO searchBundleResultDTO = response.getBody();

        return searchBundleResultDTO;
    }

    public QualifiedNameDTO fetchPreferredBundleVersion(QualifiedName bundleId, String versionpreference) throws IOException {
        PickVersionParamsDTO params = new PickVersionParamsDTO(
                new QualifiedNameDTO().from(bundleId),
                versionpreference);

        String serviceUri = provServiceTable.getServiceUri(bundleId.getUri());
        if (serviceUri == null) {
            throw new IOException("No prov service found for bundle: " + bundleId.getUri());
        }

        String url = serviceUri + "/api/pickVersion";

        System.out.println("Fetching pick version result for bundle " + bundleId.getUri() + " via: " + url + " with version preference: " + versionpreference);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PickVersionParamsDTO> request = new HttpEntity<>(params, headers);

        ResponseEntity<QualifiedNameDTO> response = restTemplate.postForEntity(
                url, request, QualifiedNameDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("API call failed with status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private ECredibility mergeCredibility(ECredibility bundleCredibility, ECredibility pathCredibility) {
        if (bundleCredibility == ECredibility.INVALID) return ECredibility.INVALID;
        if (bundleCredibility == ECredibility.VALID && pathCredibility == ECredibility.VALID) return ECredibility.VALID;
        return ECredibility.LOW;
    }
}
