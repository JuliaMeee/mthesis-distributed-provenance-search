package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.BundleTraverser;

import java.util.List;

public class FindFittingNodes implements IFindableInDocument<INode> {
    public ICondition<INode> nodeCondition;
    public ICondition<EdgeToNode> pathCondition;

    public FindFittingNodes() {
    }

    public FindFittingNodes(ICondition<INode> nodeCondition) {
        this.nodeCondition = nodeCondition;
        this.pathCondition = null;
    }

    public FindFittingNodes(ICondition<INode> nodeCondition, ICondition<EdgeToNode> pathCondition) {
        this.nodeCondition = nodeCondition;
        this.pathCondition = pathCondition;
    }

    @Override
    public List<INode> find(CpmDocument document, INode startNode) {
        return BundleTraverser.traverseAndFindNodes(startNode, node -> nodeCondition == null || nodeCondition.test(node), pathCondition);
    }
}
