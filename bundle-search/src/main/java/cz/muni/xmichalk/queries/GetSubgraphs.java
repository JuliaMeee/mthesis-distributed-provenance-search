package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import cz.muni.xmichalk.util.ProvJsonUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GetSubgraphs implements IQuery<List<JsonNode>> {
    public IFindableInDocument<List<EdgeToNode>> subgraphFinder;

    public GetSubgraphs() {
    }

    public GetSubgraphs(IFindableInDocument<List<EdgeToNode>> subgraphFinder) {
        this.subgraphFinder = subgraphFinder;
    }

    @Override
    public List<JsonNode> evaluate(BundleStart input) {
        if (subgraphFinder == null) {
            return null;
        }

        List<List<EdgeToNode>> foundSubgraphs = subgraphFinder.find(input.bundle, input.startNode);

        if (foundSubgraphs == null || foundSubgraphs.isEmpty()) {
            return null;
        }

        return foundSubgraphs.stream().map(subgraph -> transformSubgraphToDocJson(subgraph)).collect(Collectors.toList());
    }

    private JsonNode transformSubgraphToDocJson(Collection<EdgeToNode> subgraph) {
        if (subgraph == null || subgraph.isEmpty()) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        List<INode> nodes = subgraph.stream().map(edgeToNode -> edgeToNode.node).toList();
        List<IEdge> edges = subgraph.stream().map(edgeToNode -> edgeToNode.edge).toList();

        Document subgraphDocument = ProvDocumentUtils.encapsulateInDocument(nodes, edges);
        String jsonString = ProvDocumentUtils.serialize(subgraphDocument, Formats.ProvFormat.JSON);
        jsonString = ProvJsonUtils.removeExplicitBundleId(jsonString);
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert subgraph document to JsonNode", e);
        }
    }
}
