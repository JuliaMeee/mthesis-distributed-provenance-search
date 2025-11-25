package cz.muni.xmichalk.querySpecification.findable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FindFittingNodes.class, name = "FindFittingNodes"),
        @JsonSubTypes.Type(value = FindFittingLinearSubgraphs.class, name = "FindFittingLinearSubgraphs"),
})
public interface IFindableInDocument<T> {
    List<T> find(CpmDocument document, INode startNode);
}
