package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.bundleSearch.ETargetType;

public class SearchParams {
    public QualifiedNameData bundleId;
    public QualifiedNameData startNodeId;
    public ETargetType targetType;
    public JsonNode targetSpecification;

    public SearchParams() {
    }

    public SearchParams(QualifiedNameData bundleId, QualifiedNameData startNodeId, ETargetType targetType, JsonNode targetSpecification) {
        this.bundleId = bundleId;
        this.startNodeId = startNodeId;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}