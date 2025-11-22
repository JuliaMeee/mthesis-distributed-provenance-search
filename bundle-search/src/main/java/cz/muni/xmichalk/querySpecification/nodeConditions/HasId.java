package cz.muni.xmichalk.querySpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;

public class HasId implements ICondition<INode> {
    public String idUriRegex;

    public HasId() {
    }

    public HasId(String idUriRegex) {
        this.idUriRegex = idUriRegex;
    }

    @Override
    public boolean test(INode node) {
        if (idUriRegex == null) return true;
        return node.getId().getUri().matches(idUriRegex);
    }
}
