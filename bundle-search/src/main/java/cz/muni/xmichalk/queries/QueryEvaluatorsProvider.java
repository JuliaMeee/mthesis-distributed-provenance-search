package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.queries.queryEvaluators.FindConnectors;
import cz.muni.xmichalk.queries.queryEvaluators.FindInBundle;
import cz.muni.xmichalk.queries.queryEvaluators.TestFits;

import java.util.List;
import java.util.Map;

public class QueryEvaluatorsProvider {
    public static Map<EQueryType, IQueryEvaluator<?>> getQueryEvaluators() {
        return Map.of(
                EQueryType.NODE_IDS, new FindInBundle<INode, List<QualifiedNameData>>(
                        ResultTransformation::transformNodesToIds
                ),
                EQueryType.NODES, new FindInBundle<INode, JsonNode>(
                        ResultTransformation::transformNodesToDocJson
                ),
                EQueryType.SUBGRAPHS, new FindInBundle<List<EdgeToNode>, List<JsonNode>>(
                        (x) -> ResultTransformation.transformCollection(x, ResultTransformation::transformSubgraphToDocJson)
                ),
                EQueryType.CONNECTORS, new FindConnectors(),
                EQueryType.TEST_FITS, new TestFits()
        );
    }
}
