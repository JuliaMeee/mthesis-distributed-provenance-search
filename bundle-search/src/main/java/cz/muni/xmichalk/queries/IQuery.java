package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetConnectors.class, name = "GetConnectors"),
        @JsonSubTypes.Type(value = GetNodeIds.class, name = "GetNodeIds"),
        @JsonSubTypes.Type(value = GetNodes.class, name = "GetNodes"),
        @JsonSubTypes.Type(value = GetSubgraphs.class, name = "GetSubgraphs"),
        @JsonSubTypes.Type(value = TestBundleFits.class, name = "TestBundleFits"),
})
public interface IQuery<T> {
    T evaluate(CpmDocument document, INode startNode);
}
