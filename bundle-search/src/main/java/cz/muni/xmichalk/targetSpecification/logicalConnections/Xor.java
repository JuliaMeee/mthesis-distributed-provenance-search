package cz.muni.xmichalk.targetSpecification.logicalConnections;

import cz.muni.xmichalk.targetSpecification.ITestableSpecification;

public class Xor<T> implements ITestableSpecification<T> {
    public ITestableSpecification<T> first;
    public ITestableSpecification<T> second;

    public Xor() {
    }

    public Xor(ITestableSpecification<T> first, ITestableSpecification<T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean test(T item) {

        if (first == null || second == null) {
            throw new IllegalStateException("Both specifications must be non-null for XOR operation.");
        }

        return first.test(item) ^ second.test(item);
    }
}
