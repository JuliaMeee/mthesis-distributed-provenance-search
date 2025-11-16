package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.util.BundleNodesTraverser;
import cz.muni.xmichalk.util.CpmUtils;

import java.util.List;

public class CountNodes implements ICountableInDocument {
    public ITestableSpecification<INode> nodePredicate;

    public CountNodes() {
    }

    public CountNodes(ITestableSpecification<INode> nodePredicate) {
        this.nodePredicate = nodePredicate;
    }

    @Override
    public int countInDocument(CpmDocument bundle) {
        INode startNode = CpmUtils.chooseStartNode(bundle);

        List<INode> fittingNodes = BundleNodesTraverser.traverseAndFind(startNode, node -> nodePredicate.test(node));

        return fittingNodes.size();
    }
}
