package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.BundleNodesTraverser;

import java.util.List;

public class FindFittingNodes implements IFindableInDocument<INode> {
    public ICondition<INode> nodePredicate;

    public FindFittingNodes() {
    }

    public FindFittingNodes(ICondition<INode> nodePredicate) {
        this.nodePredicate = nodePredicate;
    }

    @Override
    public List<INode> find(INode startNode) {
        return BundleNodesTraverser.traverseAndFind(startNode, node -> nodePredicate.test(node));
    }
}
