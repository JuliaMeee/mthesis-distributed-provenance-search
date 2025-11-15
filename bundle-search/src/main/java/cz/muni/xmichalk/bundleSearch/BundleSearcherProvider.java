package cz.muni.xmichalk.bundleSearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.bundleSearch.searchImplementations.FindBundle;
import cz.muni.xmichalk.bundleSearch.searchImplementations.FindConnectors;
import cz.muni.xmichalk.bundleSearch.searchImplementations.FindNodes;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.targetSpecification.ITestableSpecification;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.Map;

public class BundleSearcherProvider {
    public static Map<ETargetType, ISearchBundle<?>> getBundleSearchers() {
        return Map.of(
                ETargetType.NODE_IDS_BY_ID, new FindNodes<List<QualifiedNameData>>(
                        FindNodes::translateNodeIdToPredicate,
                        FindNodes::transformResultsToIds
                ),
                ETargetType.NODES_BY_ID, new FindNodes<JsonNode>(
                        FindNodes::translateNodeIdToPredicate,
                        FindNodes::transformResultsToDocJson
                ),
                ETargetType.NODE_IDS_BY_SPECIFICATION, new FindNodes<List<QualifiedNameData>>(
                        FindNodes::translateNodeSpecificationToPredicate,
                        FindNodes::transformResultsToIds
                ),
                ETargetType.NODES_BY_SPECIFICATION, new FindNodes<JsonNode>(
                        FindNodes::translateNodeSpecificationToPredicate,
                        FindNodes::transformResultsToDocJson
                ),
                ETargetType.CONNECTORS, new FindConnectors(),
                ETargetType.BUNDLE_ID_BY_META_BUNDLE_ID, new FindBundle<QualifiedNameData>(
                        FindBundle::translateMetaBundleIdToPredicate,
                        (CpmDocument doc) -> doc == null ? null : new QualifiedNameData(doc.getBundleId())
                ),
                ETargetType.TEST_FITS, (CpmDocument document, QualifiedName startNodeId, JsonNode targetSpecification) ->
                {
                    ObjectMapper mapper = new ObjectMapper();
                    ITestableSpecification<CpmDocument> requirement = mapper.convertValue(targetSpecification, ITestableSpecification.class);
                    return requirement.test(document);
                }
        );
    }
}
