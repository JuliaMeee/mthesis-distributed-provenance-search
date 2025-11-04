package cz.muni.xmichalk.TargetSpecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.CpmDocument;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CountNodes.class, name = "CountNodes"),
        @JsonSubTypes.Type(value = CountLinearSubgraphs.class, name = "CountLinearSubgraphs"),
})
public interface ICountableInDocument {
    int countInDocument(CpmDocument document);
}
