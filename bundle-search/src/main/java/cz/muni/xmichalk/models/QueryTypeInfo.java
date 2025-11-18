package cz.muni.xmichalk.models;

import cz.muni.xmichalk.queries.EQueryType;

public class QueryTypeInfo {
    public EQueryType queryType;
    public String description;

    public QueryTypeInfo() {
    }

    public QueryTypeInfo(EQueryType queryType, String description) {
        this.queryType = queryType;
        this.description = description;
    }
}