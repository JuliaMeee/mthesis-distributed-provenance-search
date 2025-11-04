package cz.muni.xmichalk.TargetSpecification.LogicalConnections;

import cz.muni.xmichalk.TargetSpecification.ITestableSpecification;

public class Implication<T> implements ITestableSpecification<T> {
    public ITestableSpecification<T> premise;
    public ITestableSpecification<T> consequence;

    public Implication() {
    }

    public Implication(ITestableSpecification<T> premise, ITestableSpecification<T> consequence) {
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
