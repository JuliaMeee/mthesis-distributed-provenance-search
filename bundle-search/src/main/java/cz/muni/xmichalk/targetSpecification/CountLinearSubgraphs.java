package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.LinearSubgraphFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class CountLinearSubgraphs implements ICountableInDocument {
    public NodeSpecification firstNode;
    public List<EdgeToNodeSpecification> edgesAndNodes;

    public CountLinearSubgraphs() {
    }

    public CountLinearSubgraphs(NodeSpecification firstNode, List<EdgeToNodeSpecification> edgesAndNodes) {
        this.firstNode = firstNode;
        this.edgesAndNodes = edgesAndNodes;
    }

    public List<BiPredicate<IEdge, INode>> buildSubgraphConstraints() {
        ArrayList<BiPredicate<IEdge, INode>> constraints = new ArrayList<BiPredicate<IEdge, INode>>();
        constraints.add((edge, node) -> firstNode == null || firstNode.test(node));

        if (edgesAndNodes != null) {
            for (EdgeToNodeSpecification edgeAndNode : edgesAndNodes) {
                constraints.add((edge, node) ->
                        edgeAndNode == null || edgeAndNode.test(edge, node));
            }
        }

        return constraints;
    }

    @Override
    public int countInDocument(CpmDocument document) {
        INode startNode = CpmUtils.chooseStartNode(document);

        List<BiPredicate<IEdge, INode>> constraints = buildSubgraphConstraints();

        List<List<EdgeToNode>> fittingSubgraphs = LinearSubgraphFinder.findAnywhere(startNode, constraints);

        return fittingSubgraphs.size();
    }
}
