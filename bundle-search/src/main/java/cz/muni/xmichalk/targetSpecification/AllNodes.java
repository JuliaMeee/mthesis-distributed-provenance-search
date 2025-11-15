package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;

public class AllNodes implements ITestableSpecification<CpmDocument> {
    public ITestableSpecification<INode> predicate;

    public AllNodes() {

    }

    public AllNodes(ITestableSpecification<INode> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(CpmDocument target) {
        if (predicate == null) {
            throw new IllegalStateException("Predicate must be non-null");
        }
        return target.getNodes().stream().allMatch(node -> predicate.test(node));
    }
}
