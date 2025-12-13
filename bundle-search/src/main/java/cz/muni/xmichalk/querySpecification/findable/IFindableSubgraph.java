package cz.muni.xmichalk.querySpecification.findable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.DocumentStart;
import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.countable.ICountable;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = FittingNodes.class, name = "FittingNodes"),
                @JsonSubTypes.Type(value = FittingLinearSubgraphs.class, name = "FittingLinearSubgraphs"),
                @JsonSubTypes.Type(value = FilteredSubgraphs.class, name = "FilteredSubgraphs"),
                @JsonSubTypes.Type(value = StartNode.class, name = "StartNode"),
                @JsonSubTypes.Type(value = WholeGraph.class, name = "WholeGraph"),
                @JsonSubTypes.Type(value = DerivationPathFromStartNode.class, name = "DerivationPathFromStartNode"),
        }
)
public interface IFindableSubgraph extends ICountable<DocumentStart> {
    List<SubgraphWrapper> find(SubgraphWrapper graph, INode startNode);


    default List<SubgraphWrapper> find(CpmDocument document, INode startNode) {
        return find(new SubgraphWrapper(document), startNode);
    }

    @Override default int count(DocumentStart documentStart) {
        return find(documentStart.document, documentStart.startNode).size();
    }
}
