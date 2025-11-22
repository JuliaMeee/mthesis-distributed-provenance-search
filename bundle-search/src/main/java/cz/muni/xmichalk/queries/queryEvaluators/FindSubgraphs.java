package cz.muni.xmichalk.queries.queryEvaluators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import cz.muni.xmichalk.util.ProvJsonUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class FindSubgraphs<T> implements IQueryEvaluator<T> {
    private final Function<Collection<Collection<EdgeToNode>>, T> resultTransformation;

    public FindSubgraphs(Function<Collection<Collection<EdgeToNode>>, T> resultTransformation) {
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(CpmDocument document, QualifiedName startNodeId, JsonNode querySpecification) {
        ObjectMapper objectMapper = new ObjectMapper();

        IFindableInDocument<Collection<EdgeToNode>> findable = objectMapper.convertValue(querySpecification, new TypeReference<IFindableInDocument<Collection<EdgeToNode>>>() {
        });

        INode startNode = document.getNode(startNodeId);
        if (startNode == null) {
            throw new IllegalArgumentException("Start node with id " + startNodeId.getUri() + " does not exist in document " + document.getBundleId().getUri());
        }

        Collection<Collection<EdgeToNode>> foundSubgraphs = findable.find(startNode);

        return resultTransformation.apply(foundSubgraphs);
    }

    public static List<JsonNode> transformResultsToDocJsonList(Collection<Collection<EdgeToNode>> subgraphs) {
        if (subgraphs == null || subgraphs.isEmpty()) {
            return null;
        }

        List<JsonNode> serializedSubgraphs = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();

        for (Collection<EdgeToNode> subgraph : subgraphs) {
            if (subgraph == null || subgraph.isEmpty()) {
                continue;
            }

            List<INode> nodes = subgraph.stream().map(edgeToNode -> edgeToNode.node).toList();
            List<IEdge> edges = subgraph.stream().map(edgeToNode -> edgeToNode.edge).toList();

            Document subgraphDocument = ProvDocumentUtils.encapsulateInDocument(nodes, edges);
            String jsonString = ProvDocumentUtils.serialize(subgraphDocument, Formats.ProvFormat.JSON);
            jsonString = ProvJsonUtils.removeExplicitBundleId(jsonString);
            try {
                JsonNode subgraphJsonNode = objectMapper.readTree(jsonString);
                serializedSubgraphs.add(subgraphJsonNode);
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert subgraph document to JSON", e);
            }
        }

        return serializedSubgraphs;
    }
}
