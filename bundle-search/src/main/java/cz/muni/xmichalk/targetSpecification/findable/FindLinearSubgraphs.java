package cz.muni.xmichalk.targetSpecification.findable;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.targetSpecification.ICondition;
import cz.muni.xmichalk.targetSpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.util.LinearSubgraphFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class FindLinearSubgraphs implements IFindableInDocument<List<EdgeToNode>> {
    public ICondition<INode> firstNode;
    public List<EdgeToNodeCondition> edgesAndNodes;

    public FindLinearSubgraphs() {
    }

    public FindLinearSubgraphs(ICondition<INode> firstNode, List<EdgeToNodeCondition> edgesAndNodes) {
        this.firstNode = firstNode;
        this.edgesAndNodes = edgesAndNodes;
    }

    public List<BiPredicate<IEdge, INode>> buildSubgraphConstraints() {
        ArrayList<BiPredicate<IEdge, INode>> constraints = new ArrayList<BiPredicate<IEdge, INode>>();
        constraints.add((edge, node) -> firstNode == null || firstNode.test(node));

        if (edgesAndNodes != null) {
            for (EdgeToNodeCondition edgeAndNode : edgesAndNodes) {
                constraints.add((edge, node) ->
                        edgeAndNode == null || edgeAndNode.test(edge, node));
            }
        }

        return constraints;
    }

    @Override
    public List<List<EdgeToNode>> find(INode startNode) {
        List<BiPredicate<IEdge, INode>> constraints = buildSubgraphConstraints();

        return LinearSubgraphFinder.findAnywhere(startNode, constraints);
    }
}
