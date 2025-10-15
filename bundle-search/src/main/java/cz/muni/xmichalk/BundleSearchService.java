package cz.muni.xmichalk;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.xmichalk.BundleSearcher.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearcher.IBundleSearcher;
import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DocumentLoader.DocumentWithIntegrity;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.jmx.support.ConnectorServerFactoryBean;

import java.io.IOException;

public class BundleSearchService {
    private final IDocumentLoader documentLoader;
    private final ProvFactory provFactory;
    private final ICpmFactory cpmFactory;
    private final ICpmProvFactory cpmProvFactory;
    
    public BundleSearchService(IDocumentLoader documentLoader, ProvFactory provFactory, ICpmFactory cpmFactory, ICpmProvFactory cpmProvFactory) {
        this.documentLoader = documentLoader;
        this.provFactory = provFactory;
        this.cpmFactory = cpmFactory;
        this.cpmProvFactory = cpmProvFactory;
    }

    public ResponseDTO searchBundleBackward(QualifiedName bundleId, QualifiedName forwardConnectorId, String targetType, String targetSpecification) throws IOException {
        DocumentWithIntegrity documentWithIntegrity = documentLoader.loadDocument(bundleId.getUri());
        var document = documentWithIntegrity.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        
        var searcherConstructor = BundleSearcherRegistry.getSearcherConstructor(targetType);
        IBundleSearcher searcher = searcherConstructor.apply(targetSpecification);

        Object result = searcher.search(cpmDocument, forwardConnectorId);
        var connectors = cpmDocument.getBackwardConnectors();
        
        return new ResponseDTO(connectors, searcher.serializeResult(result), document);
    }

    public ResponseDTO searchBundleBackward(Document document, QualifiedName forwardConnectorId, String targetType, String targetSpecification) throws IOException {

        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        var searcherConstructor = BundleSearcherRegistry.getSearcherConstructor(targetType);
        IBundleSearcher searcher = searcherConstructor.apply(targetSpecification);

        Object result = searcher.search(cpmDocument, forwardConnectorId);
        var connectors = cpmDocument.getBackwardConnectors();

        return new ResponseDTO(connectors, searcher.serializeResult(result), document);
    }

    public void loadMetaBundle(String uri) {
        var loadedMeta = documentLoader.loadMetaDocument(uri);
        var document = loadedMeta.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        // var integrity = loadedMeta.integrityCheckPassed ? ECredibility.VALID : ECredibility.INVALID;
    }
    
    
}
