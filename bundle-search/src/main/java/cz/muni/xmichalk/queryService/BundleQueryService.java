package cz.muni.xmichalk.queryService;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.models.QueryResult;
import cz.muni.xmichalk.queries.IQuery;
import cz.muni.xmichalk.storage.EBundlePart;
import cz.muni.xmichalk.storage.IStorage;
import cz.muni.xmichalk.storage.StorageCpmDocument;
import org.openprovenance.prov.model.QualifiedName;

import java.nio.file.AccessDeniedException;

public class BundleQueryService {
    public final IStorage documentLoader;

    public BundleQueryService(IStorage documentLoader) {
        this.documentLoader = documentLoader;
    }

    public <T> QueryResult<T> evaluateBundleQuery(QualifiedName bundleId, QualifiedName startNodeId, IQuery<T> query,
                                                  String authorizationHeader) throws AccessDeniedException {
        EBundlePart requiredBundlePart = query.decideRequiredBundlePart();
        StorageCpmDocument retrievedDocument =
                documentLoader.loadCpmDocument(bundleId.getUri(), requiredBundlePart, authorizationHeader);
        CpmDocument document = retrievedDocument.document;
        INode startNode = document.getNode(startNodeId);
        QueryContext context = new QueryContext(
                document,
                startNode,
                authorizationHeader,
                this.documentLoader
        );
        T result = query.evaluate(context);

        return new QueryResult<T>(
                result,
                retrievedDocument.token);
    }
}
