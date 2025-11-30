package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.BundleTraverser;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetFilteredSubgraph implements IQuery<JsonNode> {
    public ICondition<EdgeToNode> pathCondition;

    public GetFilteredSubgraph() {
    }

    public GetFilteredSubgraph(ICondition<EdgeToNode> pathCondition) {
        this.pathCondition = pathCondition;
    }

    @Override
    public JsonNode evaluate(final BundleStart input) {
        List<INode> nodes = new ArrayList<>();
        List<IEdge> edges = new ArrayList<>();

        BundleTraverser.traverseFrom(
                input.startNode,
                edgeToNode -> {
                    if (edgeToNode.node != null) {
                        nodes.add(edgeToNode.node);
                    }
                    if (edgeToNode.edge != null) {
                        edges.add(edgeToNode.edge);
                    }
                },
                pathCondition);

        Document encapsulatingDocument = ProvDocumentUtils.encapsulateInDocument(nodes, edges);
        String jsonString = ProvDocumentUtils.serialize(encapsulatingDocument, Formats.ProvFormat.JSON);

        try {
            return new ObjectMapper().readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert subgraph document to JsonNode", e);
        }

    }
}
