package cz.muni.xmichalk.targetSpecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.xmichalk.targetSpecification.bundleConditions.AllNodes;
import cz.muni.xmichalk.targetSpecification.bundleConditions.CountCondition;
import cz.muni.xmichalk.targetSpecification.logicalOperations.AllTrue;
import cz.muni.xmichalk.targetSpecification.logicalOperations.AnyTrue;
import cz.muni.xmichalk.targetSpecification.logicalOperations.Either;
import cz.muni.xmichalk.targetSpecification.logicalOperations.Implication;
import cz.muni.xmichalk.targetSpecification.nodeConditions.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HasAttr.class, name = "HasAttr"),
        @JsonSubTypes.Type(value = HasAttrLangStringValue.class, name = "HasAttrLangStringValue"),
        @JsonSubTypes.Type(value = HasAttrQualifiedNameValue.class, name = "HasAttrQualifiedNameValue"),
        @JsonSubTypes.Type(value = HasAttrTimestampValue.class, name = "HasAttrTimestampValue"),
        @JsonSubTypes.Type(value = HasId.class, name = "HasId"),
        @JsonSubTypes.Type(value = IsKind.class, name = "IsKind"),
        @JsonSubTypes.Type(value = IsNotKind.class, name = "IsNotKind"),
        @JsonSubTypes.Type(value = CountCondition.class, name = "CountCondition"),
        @JsonSubTypes.Type(value = AllNodes.class, name = "AllNodes"),
        @JsonSubTypes.Type(value = AllTrue.class, name = "AllTrue"),
        @JsonSubTypes.Type(value = AnyTrue.class, name = "AnyTrue"),
        @JsonSubTypes.Type(value = Either.class, name = "Either"),
        @JsonSubTypes.Type(value = Implication.class, name = "Implication"),
})
public interface ICondition<T> {
    boolean test(T target);
}
