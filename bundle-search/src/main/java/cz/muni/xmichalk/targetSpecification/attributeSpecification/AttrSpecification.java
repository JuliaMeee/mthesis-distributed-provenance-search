package cz.muni.xmichalk.targetSpecification.attributeSpecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.fi.cpm.model.INode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = QualifiedNameAttrSpecification.class, name = "QualifiedNameAttrSpecification"),
        @JsonSubTypes.Type(value = LangStringAttrSpecification.class, name = "LangStringAttrSpecification"),
        @JsonSubTypes.Type(value = TimestampAttrSpecification.class, name = "TimestampAttrSpecification"),
})
public abstract class AttrSpecification {
    public String attributeNameUri;

    public boolean test(INode node) {
        return true;
    }

}
