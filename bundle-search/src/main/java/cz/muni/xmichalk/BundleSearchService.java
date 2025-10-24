package cz.muni.xmichalk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.template.schema.BackwardConnector;
import cz.muni.xmichalk.BundleSearch.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearch.ETargetType;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DocumentLoader.DocumentWithIntegrity;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;

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

    public ResponseDTO searchBundle(QualifiedName bundleId, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException {
        DocumentWithIntegrity documentWithIntegrity = documentLoader.loadDocument(bundleId.getUri());
        var document = documentWithIntegrity.document;
        return searchDocument(document, startNodeId, targetType, targetSpecification);
    }

    public ResponseDTO searchDocument(Document document, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException {

        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);

        var searchFunc = BundleSearcherRegistry.getSearchFunc(targetType);

        Object result = searchFunc.apply(cpmDocument, startNodeId, targetSpecification);

        ResponseDTO responseDTO = new ResponseDTO(
                new QualifiedNameDTO(cpmDocument.getBundleId()),
                new ObjectMapper().valueToTree(result));
        
        System.out.println("Response bundleId:\n" + responseDTO.bundleId().toString());
        System.out.println("Response found results:\n" + responseDTO.found());
        
        return responseDTO;
    }

    public void loadMetaBundle(String uri) {
        var loadedMeta = documentLoader.loadMetaDocument(uri);
        var document = loadedMeta.document;
        var cpmDocument = new CpmDocument(document, provFactory, cpmProvFactory, cpmFactory);
        // var integrity = loadedMeta.integrityCheckPassed ? ECredibility.VALID : ECredibility.INVALID;
    }
    
    
}
