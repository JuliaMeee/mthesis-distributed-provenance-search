package cz.muni.xmichalk.models;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.storage.IStorage;

public class QueryContext {
    public final CpmDocument document;
    public final INode startNode;
    public final String authorizationHeader;
    public final IStorage documentLoader;

    public QueryContext(CpmDocument document, INode startNode,
                        String authorizationHeader, IStorage documentLoader) {
        this.document = document;
        this.startNode = startNode;
        this.authorizationHeader = authorizationHeader;
        this.documentLoader = documentLoader;
    }
}
