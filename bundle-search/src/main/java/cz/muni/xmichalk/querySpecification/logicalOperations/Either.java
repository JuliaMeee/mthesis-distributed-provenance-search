package cz.muni.xmichalk.querySpecification.logicalOperations;

import cz.muni.xmichalk.querySpecification.ICondition;

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
            throw new IllegalStateException("Either operation must have both 'first' and 'second' specified.");
        }

        return first.test(item) ^ second.test(item);
    }
}
