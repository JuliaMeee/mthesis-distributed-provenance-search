package cz.muni.xmichalk.querySpecification.findable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.INode;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FindNodes.class, name = "FindNodes"),
        @JsonSubTypes.Type(value = FindLinearSubgraphs.class, name = "FindLinearSubgraphs"),
})
public interface IFindableInDocument<T> {
    List<T> find(INode startNode);
}
