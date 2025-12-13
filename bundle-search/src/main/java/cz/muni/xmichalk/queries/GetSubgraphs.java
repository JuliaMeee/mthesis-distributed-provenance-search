package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.findable.IFindableSubgraph;
import cz.muni.xmichalk.util.ResultsTransformationUtils;
import org.openprovenance.prov.model.Document;

import java.util.List;
import java.util.stream.Collectors;

public class GetSubgraphs implements IQuery<List<JsonNode>> {
    public IFindableSubgraph subgraph;

    public GetSubgraphs() {
    }

    public GetSubgraphs(IFindableSubgraph subgraph) {

        this.subgraph = subgraph;
    }

    @Override
    public List<JsonNode> evaluate(QueryContext context) {
        if (subgraph == null) {
            throw new IllegalStateException("Value of subgraph cannot be null  in " + this.getClass().getName());
        }

        List<SubgraphWrapper> foundSubgraphs = subgraph.find(context.document, context.startNode);

        return transformToDocJsonList(foundSubgraphs);
    }

    private List<JsonNode> transformToDocJsonList(List<SubgraphWrapper> subgraphs) {
        if (subgraphs == null || subgraphs.isEmpty()) {
            List.of();
        }

        return subgraphs.stream()
                .map(this::transformSubgraphToDocJson)
                .collect(Collectors.toList());
    }

    private JsonNode transformSubgraphToDocJson(SubgraphWrapper subgraph) {
        if (subgraph == null || subgraph.getNodes() == null) {
            List.of();
        }

        Document encapsulatigDocument = ResultsTransformationUtils.encapsulateInDocument(
                subgraph.getNodes(),
                subgraph.getEdges()
        );

        return ResultsTransformationUtils.transformToJsonNode(encapsulatigDocument);
    }
}
