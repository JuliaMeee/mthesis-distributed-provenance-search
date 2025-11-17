package cz.muni.xmichalk.targetSpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.ICondition;
import org.openprovenance.prov.model.StatementOrBundle;

public class IsKind implements ICondition<INode> {
    public StatementOrBundle.Kind kind;

    public IsKind() {
    }

    public IsKind(StatementOrBundle.Kind kind) {
        this.kind = kind;
    }

    @Override
    public boolean test(INode node) {
        if (kind == null) return true;
        return node.getKind().equals(kind);
    }
}
