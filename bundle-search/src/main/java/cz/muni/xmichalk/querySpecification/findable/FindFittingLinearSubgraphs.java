package cz.muni.xmichalk.querySpecification.findable;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.querySpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.util.LinearSubgraphFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FindFittingLinearSubgraphs implements IFindableInDocument<List<EdgeToNode>> {
    public List<ICondition<EdgeToNode>> graphParts;

    public FindFittingLinearSubgraphs() {
    }

    public FindFittingLinearSubgraphs(List<EdgeToNodeCondition> graphParts) {
        this.graphParts = new ArrayList<>(graphParts);
    }

    @Override
    public List<List<EdgeToNode>> find(CpmDocument document, INode startNode) {
        List<Predicate<EdgeToNode>> graphSpecification = List.copyOf(graphParts);

        return LinearSubgraphFinder.findAnywhere(startNode, graphSpecification);
    }
}
