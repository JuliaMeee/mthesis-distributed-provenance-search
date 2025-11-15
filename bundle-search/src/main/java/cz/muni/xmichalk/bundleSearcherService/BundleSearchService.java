package cz.muni.xmichalk.bundleSearcherService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.bundleSearch.ETargetType;
import cz.muni.xmichalk.bundleSearch.ISearchBundle;
import cz.muni.xmichalk.bundleSearch.UnsupportedTargetTypeException;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageCpmDocument;
import cz.muni.xmichalk.models.SearchResult;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BundleSearchService {
    private final IDocumentLoader documentLoader;
    private final Map<ETargetType, ISearchBundle<?>> bundleSearchers;
    private static final Logger log = LoggerFactory.getLogger(BundleSearchService.class);


    public BundleSearchService(IDocumentLoader documentLoader, Map<ETargetType, ISearchBundle<?>> bundleSearchers) {
        this.documentLoader = documentLoader;
        this.bundleSearchers = bundleSearchers;
    }

    public Map<ETargetType, ISearchBundle<?>> getBundleSearchers() {
        return bundleSearchers;
    }

    public SearchResult searchBundle(QualifiedName bundleId, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws UnsupportedTargetTypeException {
        StorageCpmDocument retrievedDocument = documentLoader.loadCpmDocument(bundleId.getUri());
        var document = retrievedDocument.document;
        var result = searchDocument(document, startNodeId, targetType, targetSpecification);

        return new SearchResult(
                retrievedDocument.token,
                new ObjectMapper().valueToTree(result));
    }

    public Object searchDocument(CpmDocument document, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws UnsupportedTargetTypeException {

        log.info("Search bundle {} starting from node {} for target type {} with specification {}", document.getBundleId().getUri(), startNodeId.getUri(), targetType, targetSpecification.toString());

        ISearchBundle<?> searchFunc = bundleSearchers.get(targetType);
        if (searchFunc == null) {
            var errorMessage = String.format("Unsupported target type: " + targetType);
            log.error(errorMessage);
            throw new UnsupportedTargetTypeException(errorMessage);
        }

        Object result = searchFunc.apply(document, startNodeId, targetSpecification);

        log.info("Search result: {}", result == null ? "null" : result.toString());


        return result;
    }
}
