package cz.muni.xmichalk.targetSpecification.logicalOperations;

import cz.muni.xmichalk.targetSpecification.ICondition;

import java.util.List;

public class AnyTrue<T> implements ICondition<T> {
    public List<ICondition<T>> conditions;

    public AnyTrue() {
    }

    public AnyTrue(List<ICondition<T>> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean test(T target) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        return conditions.stream().anyMatch(condition -> condition.test(target));
    }
}
