package cz.muni.xmichalk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.BundleSearch.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearch.ETargetType;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageCpmDocument;
import cz.muni.xmichalk.Exceptions.UnsupportedTargetTypeException;
import cz.muni.xmichalk.Models.SearchResult;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class BundleSearchService {
    private final IDocumentLoader documentLoader;
    private final BundleSearcherRegistry bundleSearcherRegistry;
    private static final Logger log = LoggerFactory.getLogger(BundleSearchService.class);


    public BundleSearchService(IDocumentLoader documentLoader, BundleSearcherRegistry bundleSearcher) {
        this.documentLoader = documentLoader;
        this.bundleSearcherRegistry = bundleSearcher;
    }

    public SearchResult searchBundle(QualifiedName bundleId, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException, UnsupportedTargetTypeException {
        StorageCpmDocument retrievedDocument = documentLoader.loadCpmDocument(bundleId.getUri());
        var document = retrievedDocument.document;
        var result = searchDocument(document, startNodeId, targetType, targetSpecification);

        return new SearchResult(
                retrievedDocument.token,
                new ObjectMapper().valueToTree(result));
    }

    public Object searchDocument(CpmDocument document, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException, UnsupportedTargetTypeException {

        log.info("Search bundle {} starting from node {} for target type {} with specification {}", document.getBundleId().getUri(), startNodeId.getUri(), targetType, targetSpecification.toString());

        Object result = bundleSearcherRegistry.search(document, startNodeId, targetType, targetSpecification);

        log.info("Search result: {}", result == null ? "null" : result.toString());


        return result;
    }

    public List<ETargetType> getSupportedTargetTypes() {
        return bundleSearcherRegistry.getAllTargetTypes();
    }

}
