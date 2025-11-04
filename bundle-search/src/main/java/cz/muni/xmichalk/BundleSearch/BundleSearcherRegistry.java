package cz.muni.xmichalk.BundleSearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindBundle;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindConnectors;
import cz.muni.xmichalk.BundleSearch.SearchImplementations.FindNodes;
import cz.muni.xmichalk.Exceptions.UnsupportedTargetTypeException;
import cz.muni.xmichalk.Models.QualifiedNameData;
import cz.muni.xmichalk.TargetSpecification.ITestableSpecification;
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
        registry.put(ETargetType.NODE_IDS_BY_SPECIFICATION,
                new FindNodes<List<QualifiedNameData>>(
                        FindNodes::translateNodeSpecificationToPredicate,
                        FindNodes::transformResultsToIds)
        );
        registry.put(ETargetType.NODES_BY_SPECIFICATION,
                new FindNodes<JsonNode>(
                        FindNodes::translateNodeSpecificationToPredicate,
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
            ITestableSpecification<CpmDocument> requirement = mapper.convertValue(targetSpecification, ITestableSpecification.class);
            return requirement.test(document);
        });

        // Add more target types here in future development
    }

    public BundleSearcherRegistry(Map<ETargetType, ISearchBundle> registry) {
        this.registry.putAll(registry);
    }

    public ISearchBundle getSearchFunc(ETargetType id) {
        return registry.get(id);

    }


    public Object search(CpmDocument document, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws UnsupportedTargetTypeException {
        ISearchBundle searcher = registry.get(targetType);
        if (searcher == null) {
            throw new UnsupportedTargetTypeException("No search function registered for target type: " + targetType);
        }
        return searcher.apply(document, startNodeId, targetSpecification);
    }

    public List<ETargetType> getAllTargetTypes() {
        return registry.keySet().stream().toList();
    }
}
