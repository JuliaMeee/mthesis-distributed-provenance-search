package cz.muni.xmichalk.Traverser;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearcher.BreadthFirstBundleSearcher;
import cz.muni.xmichalk.BundleSearcher.IBundleSearcher;
import cz.muni.xmichalk.DocumentLoader.DocumentWithIntegrity;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.Traverser.Models.*;
import org.openprovenance.prov.model.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class Traverser {
    private final IDocumentLoader documentLoader;
    private final String cpmNamespace = "https://www.commonprovenancemodel.org/";
    private final ProvFactory provFactory;
    private final ICpmFactory cpmFactory;
    private final ICpmProvFactory cpmProvFactory;

    private static final Comparator<ToSearchEntry> toSearchPriorityComparator =
            Comparator.comparing(e -> e.pathCredibility);

    public Traverser(IDocumentLoader documentLoader, ProvFactory provFactory, ICpmFactory cpmFactory, ICpmProvFactory cpmProvFactory) {
        this.documentLoader = documentLoader;
        this.provFactory = provFactory;
        this.cpmFactory = cpmFactory;
        this.cpmProvFactory = cpmProvFactory;
    }


    /***
     * Returns all accessible precursors of the given bundleId and forwardConnectorId, collecting them recursively.
     * @param startBundleId - identifier of the bundle
     * @param forwardConnectorId - identifier of the forward connector in the bundle
     * @return - list of predecessors (jsons)
     */
    public List<SearchResultEntry> searchPredecessors(QualifiedName startBundleId, QualifiedName forwardConnectorId, Predicate<INode> predicate) throws IOException {
        Set<VisitedEntry> visitedBundles = new HashSet<>();
        Queue<ToSearchEntry> bundlesToSearch = new PriorityQueue<>(toSearchPriorityComparator);
        Set<SearchResultEntry> results = new HashSet<>();

        bundlesToSearch.add(new ToSearchEntry(startBundleId, forwardConnectorId, ECredibility.VALID));

        while (!bundlesToSearch.isEmpty()) {
            var itemToSearch = bundlesToSearch.poll();
            if (visitedBundles.stream().anyMatch(e -> e.bundleId.equals(itemToSearch.bundleId))) {
                continue; // Skip already visited bundles
            }

            System.out.println("Searching bundle: " + itemToSearch.bundleId.getLocalPart());

            ProcessBundleResult processBundleResult = ProcessBundleBackward(itemToSearch.bundleId, itemToSearch.connectorId, predicate);

            results.addAll(processBundleResult.results.stream().map(node -> new SearchResultEntry(itemToSearch.bundleId, node)).toList());

            for (INode connector : processBundleResult.connectors) {
                var referencedBundleId = GetReferencedBundleId(connector);
                if (referencedBundleId != null) {
                    bundlesToSearch.add(new ToSearchEntry(referencedBundleId, connector.getId(), MergeCredibility(processBundleResult.credibility, itemToSearch.pathCredibility)));
                    System.out.println("Added bundle to search: " + referencedBundleId.getLocalPart());
                }
            }
            visitedBundles.add(new VisitedEntry(itemToSearch.bundleId, ECredibility.VALID));
        }

        return results.stream().toList();

    }

    public ProcessBundleResult ProcessBundleBackward(QualifiedName bundleId, QualifiedName forwardConnectorId, Predicate<INode> predicate) throws IOException {
        DocumentWithIntegrity documentWithIntegrity = documentLoader.loadDocument(bundleId.getUri());
        var document = documentWithIntegrity.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        var integrity = documentWithIntegrity.integrityCheckPassed ? ECredibility.VALID : ECredibility.INVALID;

        IBundleSearcher bundleSearcher = new BreadthFirstBundleSearcher();

        List<INode> results = bundleSearcher.search(cpmDocument, forwardConnectorId, predicate);
        List<INode> connectors = cpmDocument.getBackwardConnectors();

        return new ProcessBundleResult(results, connectors, integrity);
    }

    private QualifiedName GetReferencedBundleId(INode connectorNode) {
        for (Element element : connectorNode.getElements()) {
            for (Other other : element.getOther()) {
                if (IsReferencedBundleIdAttribute(other.getElementName())) {
                    return other.getValue() instanceof QualifiedName ? (QualifiedName) other.getValue() : null;
                }
            }
        }

        return null;
    }

    private boolean IsReferencedBundleIdAttribute(QualifiedName attrName) {
        return (attrName.getUri().startsWith(cpmNamespace) && attrName.getLocalPart().equals("referencedBundleId"));
    }

    private ECredibility MergeCredibility(ECredibility bundleCredibility, ECredibility pathCredibility) {
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
