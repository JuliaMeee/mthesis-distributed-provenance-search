package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.queries.queryEvaluators.FindConnectors;
import cz.muni.xmichalk.queries.queryEvaluators.FindNodes;
import cz.muni.xmichalk.queries.queryEvaluators.TestFits;

import java.util.List;
import java.util.Map;

public class QueryEvaluatorsProvider {
    public static Map<EQueryType, IQueryEvaluator<?>> getQueryEvaluators() {
        return Map.of(
                EQueryType.NODE_IDS, new FindNodes<List<QualifiedNameData>>(
                        FindNodes::transformResultsToIds
                ),
                EQueryType.NODES, new FindNodes<JsonNode>(
                        FindNodes::transformResultsToDocJson
                ),
                EQueryType.CONNECTORS, new FindConnectors(),
                EQueryType.TEST_FITS, new TestFits()
        );
    }
}
