package cz.muni.xmichalk.querySpecification.logicalOperations;

import cz.muni.xmichalk.querySpecification.ICondition;

public class Implication<T> implements ICondition<T> {
    public ICondition<T> premise;
    public ICondition<T> consequence;

    public Implication() {
    }

    public Implication(ICondition<T> premise, ICondition<T> consequence) {
        this.premise = premise;
        this.consequence = consequence;
    }

    @Override
    public boolean test(T target) {
        if (premise == null || consequence == null) {
            throw new IllegalStateException("Premise and consequence must be set before testing.");
        }
        if (premise.test(target)) {
            return consequence.test(target);
        }
        return true;
    }
}
