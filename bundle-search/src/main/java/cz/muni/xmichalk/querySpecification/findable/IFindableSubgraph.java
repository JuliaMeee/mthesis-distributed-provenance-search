package cz.muni.xmichalk.querySpecification.findable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.countable.ICountable;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FittingNodes.class, name = "FittingNodes"),
        @JsonSubTypes.Type(value = FittingLinearSubgraphs.class, name = "FittingLinearSubgraphs"),
        @JsonSubTypes.Type(value = FilteredSubgraphs.class, name = "FilteredSubgraphs"),
        @JsonSubTypes.Type(value = StartNode.class, name = "StartNode"),
        @JsonSubTypes.Type(value = WholeGraph.class, name = "WholeGraph"),
        @JsonSubTypes.Type(value = DerivationPathFromStartNode.class, name = "DerivationPathFromStartNode"),
})
public interface IFindableSubgraph extends ICountable<BundleStart> {
    List<SubgraphWrapper> find(SubgraphWrapper graph, INode startNode);


    default List<SubgraphWrapper> find(BundleStart bundleStart) {
        return find(
                new SubgraphWrapper(
                        bundleStart.bundle.getNodes(),
                        bundleStart.bundle.getEdges()
                ),
                bundleStart.startNode
        );
    }

    @Override
    default int count(BundleStart bundleStart) {
        return find(bundleStart).size();
    }
}
