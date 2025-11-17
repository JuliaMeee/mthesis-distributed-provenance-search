package cz.muni.xmichalk.targetSpecification.subgraphConditions;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.ICondition;
import org.openprovenance.prov.model.StatementOrBundle;

public class EdgeToNodeCondition {
    public StatementOrBundle.Kind isKind;
    public StatementOrBundle.Kind isNotKind;
    public Boolean nodeIsEffect;
    public ICondition<INode> nodeCondition;

    public EdgeToNodeCondition() {
    }

    public EdgeToNodeCondition(StatementOrBundle.Kind isKind, StatementOrBundle.Kind isNotKind, Boolean nodeIsEffect, ICondition<INode> nodeCondition) {
        this.isKind = isKind;
        this.isNotKind = isNotKind;
        this.nodeIsEffect = nodeIsEffect;
        this.nodeCondition = nodeCondition;
    }

    public boolean test(IEdge edge, INode node) {
        if (isKind != null) {
            if (edge.getRelations().stream().noneMatch((relation) -> relation.getKind().equals(isKind))) {
                return false;
            }
        }

        if (isNotKind != null) {
            if (edge.getRelations().stream().anyMatch((relation) -> relation.getKind().equals(isNotKind))) {
                return false;
            }
        }

        if (nodeIsEffect != null) {
            INode effectNode = edge.getEffect();
            if (nodeIsEffect && !effectNode.equals(node)) return false;
            if (!nodeIsEffect && effectNode.equals(node)) return false;
        }

        if (nodeCondition != null) {
            return nodeCondition.test(node);
        }

        return true;
    }
}
