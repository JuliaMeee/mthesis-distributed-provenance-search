package cz.muni.xmichalk.BundleSearch.SearchImplementations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearch.General.FilterNodes;
import cz.muni.xmichalk.BundleSearch.General.NodeAttributeSearcher;
import cz.muni.xmichalk.BundleSearch.ISearchBundle;
import cz.muni.xmichalk.DTO.AttributeDTO;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserialize;

public class FindNodes<T> implements ISearchBundle<T> {
    private final Function<JsonNode, Predicate<INode>> translatePredicate;
    private final Function<List<INode>, T> resultTransformation;
    
    public FindNodes(Function<JsonNode, Predicate<INode>> getPredicate, Function<List<INode>, T> resultTransformation) {
        this.translatePredicate = getPredicate;
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(final CpmDocument document, final QualifiedName startNodeId, final JsonNode targetSpecification) {
        var predicate = translatePredicate.apply(targetSpecification);
        var results = new FilterNodes().apply(document, startNodeId, predicate);
        return resultTransformation.apply(results);
    }

    public static Predicate<INode> translateNodeIdToPredicate(JsonNode targetSpecification) {
        return (node) -> Objects.equals(node.getId().getUri(), targetSpecification.asText());
    }

    public static Predicate<INode> translateNodeToPredicate(JsonNode targetSpecification) {
        try {
            var pF = new ProvFactory();
            var cPF = new CpmProvFactory(pF);
            var cF = new CpmMergedFactory();

            var doc = deserialize(targetSpecification.asText(), Formats.ProvFormat.JSON);
            var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
            var nodes = cpmDoc.getNodes();
            if (nodes.size() != 1) throw new IllegalArgumentException("target specification must contain exactly one node");
            var targetNode = nodes.getFirst();

            return (node) -> {
                var types = node.getElements().stream().map(elem -> elem.getType()).flatMap(List<Type>::stream);
                var labels = node.getElements().stream().map(elem -> elem.getLabel()).flatMap(List<LangString>::stream);
                var locations = node.getElements().stream().map(elem -> elem.getLocation()).flatMap(List<Location>::stream);
                var other = node.getElements().stream().map(elem -> elem.getOther()).flatMap(List<Other>::stream);
                List<XMLGregorianCalendar> startTimes = new ArrayList<XMLGregorianCalendar>();
                if (node.getKind() == StatementOrBundle.Kind.PROV_ACTIVITY)
                    startTimes = node.getElements().stream().map(elem -> ((Activity) elem).getStartTime()).toList();
                List<XMLGregorianCalendar> endTimes = new ArrayList<XMLGregorianCalendar>();
                if (node.getKind() == StatementOrBundle.Kind.PROV_ACTIVITY)
                    endTimes = node.getElements().stream().map(elem -> ((Activity) elem).getStartTime()).toList();

                for (Element element : targetNode.getElements()) {
                    var targetTypes = element.getType();
                    if (targetTypes != null && !targetTypes.isEmpty() &&
                            !new HashSet<>(types.toList()).containsAll(targetTypes)) {
                        return false;
                    }

                    var targetLabels = element.getLabel();
                    if (targetLabels != null && !targetLabels.isEmpty() &&
                            !new HashSet<>(labels.toList()).containsAll(targetLabels)) {
                        return false;
                    }

                    var targetLocations = element.getLocation();
                    if (targetLocations != null && !targetLocations.isEmpty() &&
                            !new HashSet<>(locations.toList()).containsAll(targetLocations)) {
                        return false;
                    }

                    var targetOther = element.getOther();
                    if (targetOther != null && !targetOther.isEmpty()
                            && !new HashSet<>(other.toList()).containsAll(targetOther)) {
                        return false;
                    }

                    if (element.getKind() == StatementOrBundle.Kind.PROV_ACTIVITY) {
                        var targetStartTime = ((Activity) element).getStartTime();
                        if (targetStartTime != null
                                && !new HashSet<>(startTimes).contains(targetStartTime)) {
                            return false;
                        }
                        var targetEndTime = ((Activity) element).getEndTime();
                        if (targetEndTime != null
                                && !new HashSet<>(endTimes).contains(targetEndTime)) {
                            return false;
                        }
                    }
                }
                return true;
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("could not parse target specification json", e);
        }
    }

    public static Predicate<INode> translateAttributesToPredicate(JsonNode targetSpecification) {
        var attributes = new ObjectMapper().convertValue(targetSpecification, new TypeReference<List<AttributeDTO>>(){});
        var nodeSearcher = new NodeAttributeSearcher();

        return (node) -> {

            for (AttributeDTO attr : attributes) {
                var attrName = new org.openprovenance.prov.vanilla.QualifiedName(attr.name().nameSpaceUri, attr.name().localPart, null);
                var value = nodeSearcher.tryGetValue(node, attrName);
                if (value == null) {
                    return false;
                } else {
                    var objectMapper = new ObjectMapper();
                    var attrValue = objectMapper.convertValue(attr.value(), value.getClass());
                    if (!value.equals(attrValue)) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    public static JsonNode transformResultsToDocJson(List<INode> nodes, CpmDocument searchedDocument) {
        if (nodes == null || nodes.isEmpty()) {return  null;}

        Document resultsDocument = ProvDocumentUtils.encapsulateInDocument(nodes, searchedDocument.getNamespaces());
        var jsonString = ProvDocumentUtils.serialize(resultsDocument, Formats.ProvFormat.JSON);
        var objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert results document to JSON", e);
        }
    }

    public static List<QualifiedNameDTO> transformResultsToIds(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {return  null;}

        return nodes.stream()
                .map(node -> new QualifiedNameDTO(node.getId()))
                .toList();
    }
}
