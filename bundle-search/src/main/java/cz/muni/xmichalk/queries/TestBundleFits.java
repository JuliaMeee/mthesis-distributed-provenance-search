package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;

public class TestBundleFits implements IQuery<Boolean> {
    public ICondition<CpmDocument> condition;

    public TestBundleFits() {
    }

    public TestBundleFits(ICondition<CpmDocument> condition) {
        this.condition = condition;
    }

    @Override
    public Boolean evaluate(CpmDocument document, INode startNode) {
        return condition == null || condition.test(document);
    }
}
