package cz.muni.xmichalk.TargetSpecification.LogicalConnections;

import cz.muni.xmichalk.TargetSpecification.ITestableSpecification;

public class And<T> implements ITestableSpecification<T> {
    public ITestableSpecification<T> first;
    public ITestableSpecification<T> second;

    public And() {
    }

    public And(ITestableSpecification<T> first, ITestableSpecification<T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean test(final T target) {
        if (first == null || second == null) {
            throw new IllegalStateException("Both specifications must be non-null for AND operation.");
        }
        return first.test(target) && second.test(target);
    }
}
