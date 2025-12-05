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

        if (condition == null) {
            throw new IllegalStateException("Value of condition cannot be null in " + this.getClass().getName());
        }
        return condition.test(input);
    }
}
