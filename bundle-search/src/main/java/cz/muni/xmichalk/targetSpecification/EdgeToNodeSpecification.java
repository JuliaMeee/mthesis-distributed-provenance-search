package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.StatementOrBundle;

public class EdgeToNodeSpecification {
    public StatementOrBundle.Kind isKind;
    public StatementOrBundle.Kind isNotKind;
    public Boolean nodeIsEffect;
    public NodeSpecification nodeSpecification;

    public EdgeToNodeSpecification() {
    }

    public EdgeToNodeSpecification(StatementOrBundle.Kind isKind, StatementOrBundle.Kind isNotKind, Boolean nodeIsEffect, NodeSpecification nodeSpecification) {
        this.isKind = isKind;
        this.isNotKind = isNotKind;
        this.nodeIsEffect = nodeIsEffect;
        this.nodeSpecification = nodeSpecification;
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
            var effectNode = edge.getEffect();
            if (nodeIsEffect && !effectNode.equals(node)) return false;
            if (!nodeIsEffect && effectNode.equals(node)) return false;
        }

        if (nodeSpecification != null) {
            return nodeSpecification.test(node);
        }

        return true;
    }
}
