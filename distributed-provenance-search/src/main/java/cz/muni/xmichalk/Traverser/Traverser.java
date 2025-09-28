package cz.muni.xmichalk.Traverser;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearcher.BreadthFirstBundleSearcher;
import cz.muni.xmichalk.BundleSearcher.IBundleSearcher;
import cz.muni.xmichalk.DocumentLoader.DocumentWithIntegrity;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.Traverser.DTO.SearchBundleResultDTO;
import cz.muni.xmichalk.Traverser.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.Models.*;
import cz.muni.xmichalk.Traverser.TraverserTable.ITraverserTable;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class Traverser {
    private final IDocumentLoader documentLoader;
    private final ProvFactory provFactory;
    private final ICpmFactory cpmFactory;
    private final ICpmProvFactory cpmProvFactory;
    private final ITraverserTable traverserTable;
    private final int concurrencyDegree = 10;

    private static final Comparator<ItemToSearch> toSearchPriorityComparator =
            Comparator.comparing(e -> e.pathCredibility);

    public Traverser(IDocumentLoader documentLoader, ProvFactory provFactory, ICpmFactory cpmFactory, ICpmProvFactory cpmProvFactory, ITraverserTable traverserTable) {
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
    public SearchResults searchPredecessors(QualifiedName startBundleId, QualifiedName forwardConnectorId, TargetSpecification targetSpecification) {
        SearchState searchState = new SearchState(
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new PriorityBlockingQueue<>(10, toSearchPriorityComparator),
                ConcurrentHashMap.newKeySet()
        );

        searchState.toSearch.add(new ItemToSearch(startBundleId, forwardConnectorId, ECredibility.VALID));

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyDegree);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicInteger runningTasks = new AtomicInteger(0);

        for (int i = 0; i < concurrencyDegree; i++) {
            ItemToSearch itemToSearch = pollNextToSearch(searchState);
            if (itemToSearch != null) {
                submitSearchTask(itemToSearch, searchState, targetSpecification,
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
                    submitSearchTask(next, searchState, targetSpecification,
                            completionService, runningTasks);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        return new SearchResults(List.copyOf(searchState.results));
    }

    private ItemToSearch pollNextToSearch(SearchState searchState) {
        ItemToSearch itemToSearch;
        while ((itemToSearch = searchState.toSearch.poll()) != null) {
            if (itemToSearch.pathCredibility != ECredibility.VALID && searchState.processing.values().stream()
                    .anyMatch(item -> item.pathCredibility == ECredibility.VALID)) {
                return null; // wait untill all valid bundles are processed before continuing with lower credibility ones
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
                                  TargetSpecification targetSpecification,
                                  CompletionService<Void> completionService,
                                  AtomicInteger runningTasks) {
        runningTasks.incrementAndGet();
        completionService.submit(() -> {
            System.out.println("Processing bundle: " + itemToSearch.bundleId + " via connector: " + itemToSearch.connectorId + " (running tasks: " + itemToSearch + ")");
            SearchBundleResult searchBundleResult = new SearchBundleResult(List.of(), List.of(), ECredibility.INVALID); // default value
            try {
                searchBundleResult = fetchSearchBundleResult(itemToSearch.bundleId, itemToSearch.connectorId, targetSpecification);
            } catch (Exception e) {
                System.err.println("Error during search bundle " + itemToSearch.bundleId.getLocalPart() + ": " + e.getMessage());
            } finally {
                searchState.visited.put(itemToSearch.bundleId, new VisitedItem(itemToSearch.bundleId, itemToSearch.pathCredibility, searchBundleResult.credibility));
                searchState.processing.remove(itemToSearch.bundleId, itemToSearch);

            }

            ECredibility mergedCredibility = mergeCredibility(searchBundleResult.credibility, itemToSearch.pathCredibility);

            searchState.results.addAll(searchBundleResult.results.stream()
                    .map(nodeId -> new FoundResult(itemToSearch.bundleId, nodeId, mergedCredibility))
                    .toList());

            for (ConnectorNode connector : searchBundleResult.connectors) {
                if (connector.referencedBundleId != null) {
                    searchState.toSearch.add(
                            new ItemToSearch(
                                    connector.referencedBundleId,
                                    connector.id,
                                    mergedCredibility
                            )
                    );
                }
            }

            System.out.println("Finished processing bundle: " + itemToSearch.bundleId + " via connector: " + itemToSearch.connectorId);

            return null;
        });
    }

    public SearchBundleResult fetchSearchBundleResult(QualifiedName bundleId, QualifiedName connectorId, TargetSpecification targetSpecification) throws IOException {
        SearchParamsDTO searchParams = new SearchParamsDTO(bundleId, connectorId, targetSpecification);
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

        ResponseEntity<SearchBundleResultDTO> response = restTemplate.postForEntity(
                url, request, SearchBundleResultDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("API call failed with status: " + response.getStatusCode());
        }

        SearchBundleResultDTO searchBundleResultDTO = response.getBody();

        return searchBundleResultDTO.toDomainModel();
    }

    public SearchBundleResult searchBundleBackward(QualifiedName bundleId, QualifiedName forwardConnectorId, Predicate<INode> predicate) {
        DocumentWithIntegrity documentWithIntegrity = documentLoader.loadDocument(bundleId.getUri());
        var document = documentWithIntegrity.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        var integrity = documentWithIntegrity.integrityCheckPassed ? ECredibility.VALID : ECredibility.INVALID;

        IBundleSearcher bundleSearcher = new BreadthFirstBundleSearcher();

        List<INode> results = bundleSearcher.search(cpmDocument, forwardConnectorId, predicate);
        List<INode> connectors = cpmDocument.getBackwardConnectors();

        return new SearchBundleResult(results, connectors, integrity);
    }

    private ECredibility mergeCredibility(ECredibility bundleCredibility, ECredibility pathCredibility) {
        if (bundleCredibility == ECredibility.INVALID) return ECredibility.INVALID;
        if (bundleCredibility == ECredibility.VALID && pathCredibility == ECredibility.VALID) return ECredibility.VALID;
        return ECredibility.LOW;
    }

    public void loadMetaBundle(String uri) {
        var loadedMeta = documentLoader.loadMetaDocument(uri);
        var document = loadedMeta.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        var integrity = loadedMeta.integrityCheckPassed ? ECredibility.VALID : ECredibility.INVALID;
    }
}
