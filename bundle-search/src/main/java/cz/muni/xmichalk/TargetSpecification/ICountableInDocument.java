package cz.muni.xmichalk.TargetSpecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.INode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeSpecification.class, name = "NodeSpecification"),
        @JsonSubTypes.Type(value = LinearSubgraphSpecification.class, name = "LinearSubgraphSpecification"),
})
public interface ICountableInDocument {
    int countInDocument(INode startNode);
}
