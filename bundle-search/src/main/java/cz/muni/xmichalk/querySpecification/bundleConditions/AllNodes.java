package cz.muni.xmichalk.querySpecification.bundleConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.querySpecification.ICondition;

public class AllNodes implements ICondition<BundleStart> {
    public ICondition<INode> predicate;

    public AllNodes() {

    }

    public AllNodes(ICondition<INode> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(BundleStart target) {
        if (predicate == null) {
            throw new IllegalStateException("Predicate must be non-null");
        }
        return target.bundle.getNodes().stream().allMatch(node -> predicate.test(node));
    }
}
