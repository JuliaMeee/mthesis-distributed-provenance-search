package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.storage.EBundlePart;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetConnectors.class, name = "GetConnectors"),
        @JsonSubTypes.Type(value = GetNodeIds.class, name = "GetNodeIds"),
        @JsonSubTypes.Type(value = GetNodes.class, name = "GetNodes"),
        @JsonSubTypes.Type(value = GetSubgraphs.class, name = "GetSubgraphs"),
        @JsonSubTypes.Type(value = TestBundleFits.class, name = "TestBundleFits"),
        @JsonSubTypes.Type(value = GetPreferredVersion.class, name = "GetPreferredVersion"),
})
public interface IQuery<T> {
    T evaluate(QueryContext context);

    default EBundlePart decideRequiredBundlePart() {
        return EBundlePart.Whole;
    }

}
