package cz.muni.xmichalk.bundleSearch;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.bundleSearch.searchImplementations.FindConnectors;
import cz.muni.xmichalk.bundleSearch.searchImplementations.FindNodes;
import cz.muni.xmichalk.bundleSearch.searchImplementations.TestFits;
import cz.muni.xmichalk.models.QualifiedNameData;

import java.util.List;
import java.util.Map;

public class BundleSearcherProvider {
    public static Map<ETargetType, ISearchBundle<?>> getBundleSearchers() {
        return Map.of(
                ETargetType.NODE_IDS, new FindNodes<List<QualifiedNameData>>(
                        FindNodes::transformResultsToIds
                ),
                ETargetType.NODES, new FindNodes<JsonNode>(
                        FindNodes::transformResultsToDocJson
                ),
                ETargetType.CONNECTORS, new FindConnectors(),
                ETargetType.TEST_FITS, new TestFits()
        );
    }
}
