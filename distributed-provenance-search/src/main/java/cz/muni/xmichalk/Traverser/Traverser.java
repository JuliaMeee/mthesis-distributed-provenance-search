package cz.muni.xmichalk.Traverser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.Traverser.DTO.BundleSearchResponseDTO;
import cz.muni.xmichalk.Traverser.DTO.ConnectorDTO;
import cz.muni.xmichalk.Traverser.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.Models.*;
import cz.muni.xmichalk.Traverser.ProvServiceTable.IProvServiceTable;
import org.openprovenance.prov.model.ProvFactory;
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
    private final IDocumentLoader documentLoader;
    private final ProvFactory provFactory;
    private final ICpmFactory cpmFactory;
    private final ICpmProvFactory cpmProvFactory;
    private final IProvServiceTable traverserTable;
    private final int concurrencyDegree = 10;

    private static final Comparator<ItemToSearch> toSearchPriorityComparator =
            Comparator.comparing(e -> !e.pathHasIntegrity);

    public Traverser(IDocumentLoader documentLoader, ProvFactory provFactory, ICpmFactory cpmFactory, ICpmProvFactory cpmProvFactory, IProvServiceTable traverserTable) {
        this.documentLoader = documentLoader;
        this.provFactory = provFactory;
        this.cpmFactory = cpmFactory;
        this.cpmProvFactory = cpmProvFactory;
        this.traverserTable = traverserTable;
    }


    /***
     * Returns all accessible precursors of the given bundleId and forwardConnectorId, collecting them recursively.
     * @param startBundleId - identifier of the bundle
     * @param forwardConnectorId - identifier of the forward connector in the bundle
     * @param targetSpecification - characterization of the nodes we are searching for
     * @return - list of predecessors (jsons)
     */
    public List<FoundResult> searchPredecessors(QualifiedName startBundleId, QualifiedName forwardConnectorId, String targetType, JsonNode targetSpecification) {
        SearchState searchState = new SearchState(
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new PriorityBlockingQueue<>(10, toSearchPriorityComparator),
                ConcurrentHashMap.newKeySet()
        );

        searchState.toSearch.add(new ItemToSearch(startBundleId, forwardConnectorId));

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyDegree);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicInteger runningTasks = new AtomicInteger(0);

        for (int i = 0; i < concurrencyDegree; i++) {
            ItemToSearch itemToSearch = pollNextToSearch(searchState);
            if (itemToSearch != null) {
                submitSearchTask(itemToSearch, searchState, targetType, targetSpecification,
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
                    submitSearchTask(next, searchState, targetType, targetSpecification,
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
            if (!itemToSearch.pathHasIntegrity && searchState.processing.values().stream()
                    .anyMatch(item -> item.pathHasIntegrity)) {
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
                                  String targetType,
                                  JsonNode targetSpecification,
                                  CompletionService<Void> completionService,
                                  AtomicInteger runningTasks) {
        runningTasks.incrementAndGet();
        completionService.submit(() -> {
            System.out.println("Processing bundle: " + itemToSearch.bundleId + " via connector: " + itemToSearch.connectorId + " (running tasks: " + itemToSearch + ")");
            BundleSearchResponseDTO findTargetResult = new BundleSearchResponseDTO(null, null); // default value
            BundleSearchResponseDTO findConnectorsResult = new BundleSearchResponseDTO(null, null);
            try {
                findTargetResult = fetchSearchBundleResult(itemToSearch.bundleId, itemToSearch.connectorId, targetType, targetSpecification);
                var newResult = getNewResult(findTargetResult);
                if (newResult != null) searchState.results.add(newResult);
                System.err.println("Got result? :" + (newResult != null));

                findConnectorsResult = fetchSearchBundleResult(itemToSearch.bundleId, itemToSearch.connectorId, "connectors", new ObjectMapper().valueToTree("backward"));
                var newItemsToSearch = getNewItemsToSearch(findConnectorsResult);
                searchState.toSearch.addAll(newItemsToSearch);
                System.err.println("Got connectors: " + newItemsToSearch.size());

                // TODO integrity and validity

            } catch (Exception e) {
                System.err.println("Error during search bundle " + itemToSearch.bundleId.getLocalPart() + ": " + e.getMessage());
            } finally {
                searchState.visited.put(itemToSearch.bundleId, new VisitedItem(itemToSearch.bundleId));
                searchState.processing.remove(itemToSearch.bundleId, itemToSearch);

            }

            System.out.println("Finished processing bundle: " + itemToSearch.bundleId + " via connector: " + itemToSearch.connectorId);

            return null;
        });
    }

    private FoundResult getNewResult(BundleSearchResponseDTO searchBundleResult) {
        if (searchBundleResult == null || searchBundleResult.found == null || searchBundleResult.found.isNull()) {
            return null;
        }

        return new FoundResult(searchBundleResult.bundleId.toDomainModel(), searchBundleResult.found); // TODO validity and integrity
    }

    private List<ItemToSearch> getNewItemsToSearch(BundleSearchResponseDTO searchBundleResult) {
        if (searchBundleResult == null || searchBundleResult.found == null || searchBundleResult.found.isNull()) {
            System.out.println("No connectors found");
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

        System.out.println("Connector dtos count: " + connectors.size());

        List<ItemToSearch> newItemsToSearch = new ArrayList<>();

        for (ConnectorDTO connector : connectors) {
            if (connector != null && connector.referencedBundleId != null) {
                System.out.println("Creating item to search");
                newItemsToSearch.add(
                        new ItemToSearch(
                                connector.referencedBundleId.toDomainModel(),
                                connector.id.toDomainModel()
                                // TODO validity and integrity
                        )
                );
            }
        }

        return newItemsToSearch;
    }

    public BundleSearchResponseDTO fetchSearchBundleResult(QualifiedName bundleId, QualifiedName connectorId, String targetType, JsonNode targetSpecification) throws IOException {
        SearchParamsDTO searchParams = new SearchParamsDTO(bundleId, connectorId, targetType, targetSpecification);
        String traverserAddress = traverserTable.getTraverserUrl(bundleId.getUri());
        if (traverserAddress == null) {
            throw new IOException("No traverser found for bundle: " + bundleId.getUri());
        }

        String url = traverserAddress + "/api/searchBundleBackward";

        System.out.println("Fetching results for bundle via: " + url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SearchParamsDTO> request = new HttpEntity<>(searchParams, headers);

        ResponseEntity<BundleSearchResponseDTO> response = restTemplate.postForEntity(
                url, request, BundleSearchResponseDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("API call failed with status: " + response.getStatusCode());
        }

        BundleSearchResponseDTO searchBundleResultDTO = response.getBody();

        return searchBundleResultDTO;
    }

    private ECredibility mergeCredibility(ECredibility bundleCredibility, ECredibility pathCredibility) {
        if (bundleCredibility == ECredibility.INVALID) return ECredibility.INVALID;
        if (bundleCredibility == ECredibility.VALID && pathCredibility == ECredibility.VALID) return ECredibility.VALID;
        return ECredibility.LOW;
    }
}
