package cz.muni.xmichalk.targetSpecification.logicalOperations;

import cz.muni.xmichalk.targetSpecification.ICondition;

public class Either<T> implements ICondition<T> {
    public ICondition<T> first;
    public ICondition<T> second;

    public Either() {
    }

    public Either(ICondition<T> first, ICondition<T> second) {
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
