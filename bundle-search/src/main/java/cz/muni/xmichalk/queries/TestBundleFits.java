package cz.muni.xmichalk.queries;

import cz.muni.xmichalk.models.DocumentStart;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.querySpecification.ICondition;

public class TestBundleFits implements IQuery<Boolean> {
    public ICondition<DocumentStart> condition;

    public TestBundleFits() {
    }

    public TestBundleFits(ICondition<DocumentStart> condition) {
        this.condition = condition;
    }

    @Override
    public Boolean evaluate(QueryContext context) {

        if (condition == null) {
            throw new IllegalStateException("Value of condition cannot be null in " + this.getClass().getName());
        }
        return condition.test(new DocumentStart(context.document, context.startNode));
    }
}
