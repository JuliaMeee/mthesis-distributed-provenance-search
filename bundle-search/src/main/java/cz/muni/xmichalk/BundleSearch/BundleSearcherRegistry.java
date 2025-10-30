package cz.muni.xmichalk.BundleSearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindBundle;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindConnectors;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindNodes;
import cz.muni.xmichalk.Models.QualifiedNameData;
import cz.muni.xmichalk.TargetSpecification.BundleSpecification;
import org.openprovenance.prov.model.QualifiedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleSearcherRegistry {
    private final Map<ETargetType, ISearchBundle> registry = new HashMap<>();

    public BundleSearcherRegistry() {
        registry.put(ETargetType.NODE_IDS_BY_ID,
                new FindNodes<List<QualifiedNameData>>(
                        FindNodes::translateNodeIdToPredicate,
                        FindNodes::transformResultsToIds)
        );
        registry.put(ETargetType.NODES_BY_ID,
                new FindNodes<JsonNode>(
                        FindNodes::translateNodeIdToPredicate,
                        FindNodes::transformResultsToDocJson)
        );
        registry.put(ETargetType.NODE_IDS_BY_ATTRIBUTES,
                new FindNodes<List<QualifiedNameData>>(
                        FindNodes::translateNodeToPredicate,
                        FindNodes::transformResultsToIds)
        );
        registry.put(ETargetType.NODES_BY_ATTRIBUTES,
                new FindNodes<JsonNode>(
                        FindNodes::translateNodeToPredicate,
                        FindNodes::transformResultsToDocJson)
        );
        registry.put(ETargetType.CONNECTORS,
                new FindConnectors()
        );
        registry.put(ETargetType.BUNDLE_ID_BY_META_BUNDLE_ID,
                new FindBundle<QualifiedNameData>(
                        FindBundle::translateMetaBundleIdToPredicate,
                        (CpmDocument doc) -> doc == null ? null : new QualifiedNameData(doc.getBundleId()))
        );
        registry.put(ETargetType.TEST_FITS, (CpmDocument document, QualifiedName startNodeId, JsonNode targetSpecification) -> {
            ObjectMapper mapper = new ObjectMapper();
            BundleSpecification requirement = mapper.convertValue(targetSpecification, new TypeReference<BundleSpecification>() {
            });
            return requirement.test(document, startNodeId);
        });

        // Add more target types here in future development
    }

    public BundleSearcherRegistry(Map<ETargetType, ISearchBundle> registry) {
        this.registry.putAll(registry);
    }

    public ISearchBundle getSearchFunc(ETargetType id) {
        return registry.get(id);

    }

    public List<ETargetType> getAllTargetTypes() {
        return registry.keySet().stream().toList();
    }
}
