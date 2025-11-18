package cz.muni.xmichalk.queryService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageCpmDocument;
import cz.muni.xmichalk.models.QueryResult;
import cz.muni.xmichalk.queries.EQueryType;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import cz.muni.xmichalk.queries.UnsupportedQueryTypeException;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BundleQueryService {
    private final IDocumentLoader documentLoader;
    private final Map<EQueryType, IQueryEvaluator<?>> queryEvaluators;
    private static final Logger log = LoggerFactory.getLogger(BundleQueryService.class);


    public BundleQueryService(IDocumentLoader documentLoader, Map<EQueryType, IQueryEvaluator<?>> queryEvaluators) {
        this.documentLoader = documentLoader;
        this.queryEvaluators = queryEvaluators;
    }

    public Map<EQueryType, IQueryEvaluator<?>> getQueryEvaluators() {
        return queryEvaluators;
    }

    public QueryResult evaluateBundleQuery(QualifiedName bundleId, QualifiedName startNodeId, EQueryType queryType, JsonNode querySpecification) throws UnsupportedQueryTypeException {
        StorageCpmDocument retrievedDocument = documentLoader.loadCpmDocument(bundleId.getUri());
        CpmDocument document = retrievedDocument.document;
        Object result = evaluateDocumentQuery(document, startNodeId, queryType, querySpecification);

        return new QueryResult(
                retrievedDocument.token,
                new ObjectMapper().valueToTree(result));
    }

    public Object evaluateDocumentQuery(CpmDocument document, QualifiedName startNodeId, EQueryType queryType, JsonNode querySpecification) throws UnsupportedQueryTypeException {

        log.info("Evaluate {} query on bundle {} starting from node {} with specification {}", queryType, document.getBundleId().getUri(), startNodeId.getUri(), querySpecification.toString());

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(queryType);
        if (queryEvaluator == null) {
            String errorMessage = String.format("Unsupported query type: " + queryType);
            log.error(errorMessage);
            throw new UnsupportedQueryTypeException(errorMessage);
        }

        Object result = queryEvaluator.apply(document, startNodeId, querySpecification);

        log.info("Query result: {}", result == null ? "null" : result.toString());


        return result;
    }
}
