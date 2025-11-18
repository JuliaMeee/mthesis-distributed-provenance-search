package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.queries.EQueryType;

public class QueryParams {
    public QualifiedNameData bundleId;
    public QualifiedNameData startNodeId;
    public EQueryType queryType;
    public JsonNode querySpecification;

    public QueryParams() {
    }

    public QueryParams(QualifiedNameData bundleId, QualifiedNameData startNodeId, EQueryType queryType, JsonNode querySpecification) {
        this.bundleId = bundleId;
        this.startNodeId = startNodeId;
        this.queryType = queryType;
        this.querySpecification = querySpecification;
    }
}