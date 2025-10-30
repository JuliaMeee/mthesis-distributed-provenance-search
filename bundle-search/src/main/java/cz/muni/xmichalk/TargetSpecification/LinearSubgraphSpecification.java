package cz.muni.xmichalk.TargetSpecification;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.General.LinearSubgraphFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class LinearSubgraphSpecification implements ICountableInDocument {
    public NodeSpecification firstNode;
    public List<EdgeToNodeSpecification> edgesAndNodes;

    public LinearSubgraphSpecification() {
    }

    public LinearSubgraphSpecification(NodeSpecification firstNode, List<EdgeToNodeSpecification> edgesAndNodes) {
        this.firstNode = firstNode;
        this.edgesAndNodes = edgesAndNodes;
    }

    public List<BiPredicate<IEdge, INode>> buildSubgraphConstraints() {
        var constraints = new ArrayList<BiPredicate<IEdge, INode>>();
        constraints.add((edge, node) -> firstNode == null || firstNode.test(node));

        if (edgesAndNodes != null) {
            for (var edgeAndNode : edgesAndNodes) {
                constraints.add((edge, node) ->
                        edgeAndNode == null || edgeAndNode.test(edge, node));
            }
        }

        return constraints;
    }

    @Override
    public int countInDocument(final INode startNode) {
        var constraints = buildSubgraphConstraints();

        var fittingSubgraphs = LinearSubgraphFinder.findAnywhere(startNode, constraints);

        return fittingSubgraphs.size();
    }
}
