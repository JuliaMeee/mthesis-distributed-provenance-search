package cz.muni.xmichalk.BundleSearch;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindBundle;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindConnectors;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindNodes;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
import org.openprovenance.prov.model.QualifiedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleSearcherRegistry {
    private static final Map<ETargetType, ISearchBundle> registry = new HashMap<>();
    // Maps target type to searcher constructor function that takes a target specification as input

    static {
        registry.put(ETargetType.NODE_IDS_BY_ID,
                new FindNodes<List<QualifiedNameDTO>>(
                        (JsonNode ts) -> FindNodes.translateNodeIdToPredicate(ts),
                        (results) -> FindNodes.transformResultsToIds(results))
        );
        registry.put(ETargetType.NODES_BY_ATTRIBUTES,
                (CpmDocument document, QualifiedName startNodeId, JsonNode targetSpecification) -> 
                        new FindNodes<JsonNode>(
                                (JsonNode ts) -> FindNodes.translateNodeToPredicate(ts), 
                                (results) -> FindNodes.transformResultsToDocJson(results, document))
        );
        registry.put(ETargetType.NODE_IDS_BY_ATTRIBUTES,
                new FindNodes<List<QualifiedNameDTO>>(
                        (JsonNode ts) -> FindNodes.translateNodeToPredicate(ts),
                        (results) -> FindNodes.transformResultsToIds(results))
        );
        registry.put(ETargetType.CONNECTORS,
                new FindConnectors()
        );
        registry.put(ETargetType.BUNDLE_ID_BY_META_BUNDLE_ID,
                new FindBundle<QualifiedNameDTO>(
                        (JsonNode ts) -> FindBundle.translateMetaBundleIdToPredicate(ts),
                        (CpmDocument doc) -> doc == null ? null : new QualifiedNameDTO(doc.getBundleId()))
        );
        registry.put(ETargetType.XXX_NODES_BY_ATTRIBUTES,
                (CpmDocument document, QualifiedName startNodeId, JsonNode targetSpecification) ->
                        new FindNodes<JsonNode>(
                                (JsonNode ts) -> FindNodes.translateAttributesToPredicate(ts),
                                (results) -> FindNodes.transformResultsToDocJson(results, document))
        );

        // Add more target types here in the future development
    }

    public static ISearchBundle getSearchFunc(ETargetType id) {
        return registry.get(id);

    }

    public static List<ETargetType> getAllTargetTypes() {
        return registry.keySet().stream().toList();
    }
}
