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
import java.util.Set;
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

    private static final Comparator<ToSearchEntry> toSearchPriorityComparator =
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
        ConcurrentMap<QualifiedName, VisitedEntry> visitedBundles = new ConcurrentHashMap<>();
        ConcurrentMap<QualifiedName, ToSearchEntry> bundlesToSearch = new ConcurrentHashMap<>();
        Set<FoundResult> results = ConcurrentHashMap.newKeySet();

        bundlesToSearch.put(startBundleId,
                new ToSearchEntry(startBundleId, forwardConnectorId, ECredibility.VALID));

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyDegree);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        AtomicInteger runningTasks = new AtomicInteger(0); // track running tasks count

        for (int i = 0; i < concurrencyDegree; i++) {
            ToSearchEntry entry = pollNextToSearch(bundlesToSearch, visitedBundles);
            if (entry != null) {
                submitSearchTask(entry, bundlesToSearch, visitedBundles, results,
                        targetSpecification, completionService, runningTasks);
            }
        }

        try {
            while (runningTasks.get() > 0) {
                // wait until one task finishes
                completionService.take();

                ToSearchEntry next;
                while (runningTasks.get() < concurrencyDegree
                        && (next = pollNextToSearch(bundlesToSearch, visitedBundles)) != null) {
                    submitSearchTask(next, bundlesToSearch, visitedBundles, results,
                            targetSpecification, completionService, runningTasks);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        return new SearchResults(List.copyOf(results));
    }

    private ToSearchEntry pollNextToSearch(
            ConcurrentMap<QualifiedName, ToSearchEntry> bundlesToSearch,
            ConcurrentMap<QualifiedName, VisitedEntry> visitedBundles
    ) {
        for (QualifiedName bundleId : bundlesToSearch.keySet()) {
            ToSearchEntry entry = bundlesToSearch.get(bundleId);
            if (entry == null) continue;

            VisitedEntry newVisited = new VisitedEntry(entry.bundleId, entry.pathCredibility, ECredibility.UNKNOWN);
            if (visitedBundles.putIfAbsent(bundleId, newVisited) == null) {
                bundlesToSearch.remove(bundleId, entry);
                return entry;
            } else {
                bundlesToSearch.remove(bundleId, entry);
            }
        }
        return null;
    }

    private void submitSearchTask(
            ToSearchEntry entry,
            ConcurrentMap<QualifiedName, ToSearchEntry> bundlesToSearch,
            ConcurrentMap<QualifiedName, VisitedEntry> visitedBundles,
            Set<FoundResult> results,
            TargetSpecification targetSpecification,
            CompletionService<Void> completionService,
            AtomicInteger runningTasks
    ) {
        var tasksCount = runningTasks.incrementAndGet();
        completionService.submit(() -> {

            try {
                System.out.println("Processing bundle: " + entry.bundleId + " via connector: " + entry.connectorId + " (running tasks: " + tasksCount + ")");

                SearchBundleResult searchBundleResult;
                try {
                    searchBundleResult = fetchSearchBundleResult(entry.bundleId, entry.connectorId, targetSpecification);
                    visitedBundles.get(entry.bundleId).bundleCredibility = searchBundleResult.credibility;
                } catch (Exception e) {
                    System.err.println("Error during search bundle " + entry.bundleId.getLocalPart()
                            + " call: " + e.getMessage());
                    searchBundleResult = new SearchBundleResult(List.of(), List.of(), ECredibility.INVALID);
                }

                ECredibility mergedCredibility = mergeCredibility(searchBundleResult.credibility, entry.pathCredibility);

                results.addAll(
                        searchBundleResult.results.stream()
                                .map(nodeId -> new FoundResult(entry.bundleId, nodeId, mergedCredibility))
                                .toList()
                );

                for (ConnectorNode connector : searchBundleResult.connectors) {
                    if (connector.referencedBundleId != null) {
                        bundlesToSearch.putIfAbsent(
                                connector.referencedBundleId,
                                new ToSearchEntry(
                                        connector.referencedBundleId,
                                        connector.id,
                                        mergedCredibility
                                )
                        );
                    }
                }

                System.out.println("Finished processing bundle: " + entry.bundleId + " via connector: " + entry.connectorId);
            } finally {
                runningTasks.decrementAndGet();
            }
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
