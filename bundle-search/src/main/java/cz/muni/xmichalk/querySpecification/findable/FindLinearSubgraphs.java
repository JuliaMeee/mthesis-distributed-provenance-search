package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.querySpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.util.LinearSubgraphFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FindLinearSubgraphs implements IFindableInDocument<List<EdgeToNode>> {
    public List<ICondition<EdgeToNode>> graphParts;

    public FindLinearSubgraphs() {
    }

    public FindLinearSubgraphs(List<EdgeToNodeCondition> graphParts) {
        this.graphParts = new ArrayList<>(graphParts);
    }

    @Override
    public List<List<EdgeToNode>> find(INode startNode) {
        List<Predicate<EdgeToNode>> graphSpecification = List.copyOf(graphParts);

        return LinearSubgraphFinder.findAnywhere(startNode, graphSpecification);
    }
}
