package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.BundleNodesTraverser;

import java.util.List;

public class FindNodes implements IFindableInDocument<INode> {
    public ICondition<INode> nodePredicate;

    public FindNodes() {
    }

    public FindNodes(ICondition<INode> nodePredicate) {
        this.nodePredicate = nodePredicate;
    }

    @Override
    public List<INode> find(INode startNode) {
        return BundleNodesTraverser.traverseAndFind(startNode, node -> nodePredicate.test(node));
    }
}
