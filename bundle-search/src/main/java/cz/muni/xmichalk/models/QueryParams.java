package cz.muni.xmichalk.models;

import cz.muni.xmichalk.queries.IQuery;

public class QueryParams {
    public QualifiedNameData bundleId;
    public QualifiedNameData startNodeId;
    public IQuery<?> querySpecification;

    public QueryParams() {
    }

    public QueryParams(QualifiedNameData bundleId, QualifiedNameData startNodeId, IQuery<?> querySpecification) {
        this.bundleId = bundleId;
        this.startNodeId = startNodeId;
        this.querySpecification = querySpecification;
    }
}