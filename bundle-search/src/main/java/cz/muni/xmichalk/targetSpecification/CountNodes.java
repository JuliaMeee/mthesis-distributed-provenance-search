package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.util.BundleNodesTraverser;
import cz.muni.xmichalk.util.CpmUtils;

public class CountNodes implements ICountableInDocument {
    public ITestableSpecification<INode> nodePredicate;

    public CountNodes() {
    }

    public CountNodes(ITestableSpecification<INode> nodePredicate) {
        this.nodePredicate = nodePredicate;
    }

    @Override
    public int countInDocument(CpmDocument bundle) {
        var startNode = CpmUtils.chooseStartNode(bundle);

        var fittingNodes = BundleNodesTraverser.traverseAndFind(startNode, node -> nodePredicate.test(node));

        return fittingNodes.size();
    }
}
