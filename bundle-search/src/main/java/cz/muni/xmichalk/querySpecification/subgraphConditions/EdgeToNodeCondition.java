package cz.muni.xmichalk.querySpecification.subgraphConditions;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.ICondition;

public class EdgeToNodeCondition implements ICondition<EdgeToNode> {
    public ICondition<IEdge> edgeCondition;
    public ICondition<INode> nodeCondition;
    public Boolean nodeIsEffect;

    public EdgeToNodeCondition() {
    }

    public EdgeToNodeCondition(ICondition<IEdge> edgeCondition, ICondition<INode> nodeCondition, Boolean nodeIsEffect) {
        this.edgeCondition = edgeCondition;
        this.nodeCondition = nodeCondition;
        this.nodeIsEffect = nodeIsEffect;
    }

    public boolean test(EdgeToNode edgeToNode) {
        if (edgeCondition != null && !edgeCondition.test(edgeToNode.edge)) {
            return false;
        }

        if (nodeCondition != null && !nodeCondition.test(edgeToNode.node)) {
            return false;
        }

        if (nodeIsEffect != null) {
            INode effectNode = edgeToNode.edge.getEffect();
            return nodeIsEffect.equals(effectNode.equals(edgeToNode.node));
        }

        return true;
    }
}
