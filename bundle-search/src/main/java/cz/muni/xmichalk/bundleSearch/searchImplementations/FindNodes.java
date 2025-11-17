package cz.muni.xmichalk.bundleSearch.searchImplementations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.bundleSearch.ISearchBundle;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.targetSpecification.ICondition;
import cz.muni.xmichalk.util.BundleNodesTraverser;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import cz.muni.xmichalk.util.ProvJsonUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class FindNodes<T> implements ISearchBundle<T> {
    private final Function<List<INode>, T> resultTransformation;

    public FindNodes(Function<List<INode>, T> resultTransformation) {
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(CpmDocument document, org.openprovenance.prov.model.QualifiedName startNodeId, JsonNode targetSpecification) {
        Predicate<INode> predicate = translateToPredicate(targetSpecification);
        List<INode> results = BundleNodesTraverser.traverseAndFind(document, startNodeId, predicate);
        return resultTransformation.apply(results);
    }

    public static Predicate<INode> translateToPredicate(JsonNode nodeSpecification) {
        ObjectMapper objectMapper = new ObjectMapper();
        ICondition<INode> specification = objectMapper.convertValue(
                nodeSpecification, new TypeReference<ICondition<INode>>() {
                });
        return specification::test;
    }

    public static JsonNode transformResultsToDocJson(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        Document resultsDocument = ProvDocumentUtils.encapsulateInDocument(nodes);
        String jsonString = ProvDocumentUtils.serialize(resultsDocument, Formats.ProvFormat.JSON);
        jsonString = ProvJsonUtils.removeExplicitBundleId(jsonString);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert results document to JSON", e);
        }
    }

    public static List<QualifiedNameData> transformResultsToIds(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        return nodes.stream()
                .map(node -> new QualifiedNameData().from(node.getId()))
                .toList();
    }
}
