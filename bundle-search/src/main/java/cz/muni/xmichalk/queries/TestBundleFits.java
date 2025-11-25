package cz.muni.xmichalk.queries;

import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.querySpecification.ICondition;

public class TestBundleFits implements IQuery<Boolean> {
    public ICondition<BundleStart> condition;

    public TestBundleFits() {
    }

    public TestBundleFits(ICondition<BundleStart> condition) {
        this.condition = condition;
    }

    @Override
    public Boolean evaluate(BundleStart input) {
        return condition == null || condition.test(input);
    }
}
