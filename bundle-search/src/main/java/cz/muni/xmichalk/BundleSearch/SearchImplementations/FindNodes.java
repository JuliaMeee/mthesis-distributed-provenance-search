package cz.muni.xmichalk.BundleSearch.SearchImplementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.General.BundleNodesTraverser;
import cz.muni.xmichalk.BundleSearch.ISearchBundle;
import cz.muni.xmichalk.Models.QualifiedNameData;
import cz.muni.xmichalk.TargetSpecification.ITestableSpecification;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import cz.muni.xmichalk.Util.ProvJsonUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class FindNodes<T> implements ISearchBundle<T> {
    private final Function<JsonNode, Predicate<INode>> translatePredicate;
    private final Function<List<INode>, T> resultTransformation;

    public FindNodes(Function<JsonNode, Predicate<INode>> getPredicate, Function<List<INode>, T> resultTransformation) {
        this.translatePredicate = getPredicate;
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(final CpmDocument document, final org.openprovenance.prov.model.QualifiedName startNodeId, final JsonNode targetSpecification) {
        var predicate = translatePredicate.apply(targetSpecification);
        var results = BundleNodesTraverser.traverseAndFind(document, startNodeId, predicate);
        return resultTransformation.apply(results);
    }

    public static Predicate<INode> translateNodeIdToPredicate(JsonNode targetSpecification) {
        return (node) -> Objects.equals(node.getId().getUri(), targetSpecification.asText());
    }

    public static Predicate<INode> translateNodeSpecificationToPredicate(JsonNode nodeSpecification) {
        ObjectMapper objectMapper = new ObjectMapper();
        ITestableSpecification<INode> specification = objectMapper.convertValue(nodeSpecification, ITestableSpecification.class);
        return specification::test;
    }

    public static JsonNode transformResultsToDocJson(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        Document resultsDocument = ProvDocumentUtils.encapsulateInDocument(nodes);
        var jsonString = ProvDocumentUtils.serialize(resultsDocument, Formats.ProvFormat.JSON);
        jsonString = ProvJsonUtils.removeExplicitBundleId(jsonString);
        var objectMapper = new ObjectMapper();
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
                .map(node -> new QualifiedNameData(node.getId()))
                .toList();
    }
}
