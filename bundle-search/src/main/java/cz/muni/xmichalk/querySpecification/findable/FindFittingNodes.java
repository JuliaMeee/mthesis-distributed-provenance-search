package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.BundleNodesTraverser;

import java.util.List;

public class FindFittingNodes implements IFindableInDocument<INode> {
    public ICondition<INode> nodeCondition;

    public FindFittingNodes() {
    }

    public FindFittingNodes(ICondition<INode> nodeCondition) {
        this.nodeCondition = nodeCondition;
    }

    @Override
    public List<INode> find(CpmDocument document, INode startNode) {
        return BundleNodesTraverser.traverseAndFind(startNode, node -> nodeCondition.test(node));
    }
}
