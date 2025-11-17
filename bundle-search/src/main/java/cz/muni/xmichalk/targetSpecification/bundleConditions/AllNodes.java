package cz.muni.xmichalk.targetSpecification.bundleConditions;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.ICondition;

public class AllNodes implements ICondition<CpmDocument> {
    public ICondition<INode> predicate;

    public AllNodes() {

    }

    public AllNodes(ICondition<INode> predicate) {
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
