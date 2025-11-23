package cz.muni.xmichalk.queryService;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageCpmDocument;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.QueryResult;
import cz.muni.xmichalk.queries.IQuery;
import cz.muni.xmichalk.queries.IRequiresDocumentLoader;
import org.openprovenance.prov.model.QualifiedName;

public class BundleQueryService {
    private final IDocumentLoader documentLoader;

    public BundleQueryService(IDocumentLoader documentLoader) {
        this.documentLoader = documentLoader;
    }

    public QueryResult evaluateBundleQuery(QualifiedName bundleId, QualifiedName startNodeId, IQuery<?> query) {
        StorageCpmDocument retrievedDocument = documentLoader.loadCpmDocument(bundleId.getUri());
        CpmDocument document = retrievedDocument.document;
        injectDependencies(query);
        Object result = query.evaluate(new BundleStart(document, document.getNode(startNodeId)));

        return new QueryResult(
                retrievedDocument.token,
                new ObjectMapper().valueToTree(result));
    }

    public void injectDependencies(IQuery<?> query) {
        if (query instanceof IRequiresDocumentLoader) {
            ((IRequiresDocumentLoader) query).injectDocumentLoader(documentLoader);
        }
    }
}
