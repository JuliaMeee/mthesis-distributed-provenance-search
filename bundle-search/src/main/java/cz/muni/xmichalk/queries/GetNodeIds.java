package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.findable.IFindableSubgraph;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetNodeIds implements IQuery<List<QualifiedNameData>> {
    public IFindableSubgraph fromSubgraphs;

    public GetNodeIds() {
    }

    public GetNodeIds(IFindableSubgraph fromSubgraphs) {
        this.fromSubgraphs = fromSubgraphs;
    }

    @Override
    public List<QualifiedNameData> evaluate(QueryContext context) {
        if (fromSubgraphs == null) {
            throw new IllegalStateException("Value of fromSubgraphs cannot be null in " + this.getClass().getName());
        }

        List<SubgraphWrapper> nodeSubgraphs = fromSubgraphs.find(context.document, context.startNode);

        return transformToNodeIds(nodeSubgraphs);
    }

    private List<QualifiedNameData> transformToNodeIds(List<SubgraphWrapper> nodeSubgraphs) {
        if (nodeSubgraphs == null || nodeSubgraphs.isEmpty()) {
            return List.of();
        }

        Set<INode> nodes = nodeSubgraphs.stream()
                .flatMap(subgraph -> subgraph.getNodes().stream()).collect(Collectors.toSet());

        if (nodes.isEmpty()) {
            return List.of();
        }

        return nodes.stream()
                .map(node -> new QualifiedNameData().from(node.getId()))
                .toList();
    }
}
