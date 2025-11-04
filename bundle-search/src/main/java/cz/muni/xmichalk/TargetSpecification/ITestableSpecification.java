package cz.muni.xmichalk.TargetSpecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.muni.xmichalk.TargetSpecification.LogicalConnections.And;
import cz.muni.xmichalk.TargetSpecification.LogicalConnections.Implication;
import cz.muni.xmichalk.TargetSpecification.LogicalConnections.Or;
import cz.muni.xmichalk.TargetSpecification.LogicalConnections.Xor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BundleSpecification.class, name = "BundleSpecification"),
        @JsonSubTypes.Type(value = NodeSpecification.class, name = "NodeSpecification"),
        @JsonSubTypes.Type(value = CountSpecification.class, name = "CountSpecification"),
        @JsonSubTypes.Type(value = AllNodes.class, name = "AllNodes"),
        @JsonSubTypes.Type(value = And.class, name = "And"),
        @JsonSubTypes.Type(value = Or.class, name = "Or"),
        @JsonSubTypes.Type(value = Xor.class, name = "Xor"),
        @JsonSubTypes.Type(value = Implication.class, name = "Implication"),
})
public interface ITestableSpecification<T> {
    boolean test(T target);
}
