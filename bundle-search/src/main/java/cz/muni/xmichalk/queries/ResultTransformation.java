package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import cz.muni.xmichalk.util.ProvJsonUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResultTransformation {

    public static <R, T> List<T> transformCollection(Collection<R> collection, Function<R, T> itemTransformation) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        return collection.stream().map(item -> itemTransformation.apply(item)).collect(Collectors.toList());
    }

    public static JsonNode transformSubgraphToDocJson(Collection<EdgeToNode> subgraph) {
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

    public static JsonNode transformNodesToDocJson(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        Document resultsDocument = ProvDocumentUtils.encapsulateInDocument(nodes, null);
        String jsonString = ProvDocumentUtils.serialize(resultsDocument, Formats.ProvFormat.JSON);
        jsonString = ProvJsonUtils.removeExplicitBundleId(jsonString);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert results document to JSON", e);
        }
    }

    public static List<QualifiedNameData> transformNodesToIds(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        return nodes.stream()
                .map(node -> new QualifiedNameData().from(node.getId()))
                .toList();
    }
}
