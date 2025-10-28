package cz.muni.xmichalk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.xmichalk.BundleSearch.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearch.ETargetType;
import cz.muni.xmichalk.BundleVersionPicker.EVersionPreferrence;
import cz.muni.xmichalk.BundleVersionPicker.VersionPickerRegistry;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DocumentLoader.StorageCpmDocument;
import cz.muni.xmichalk.DocumentLoader.StorageDocument;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.Exceptions.UnsupportedTargetTypeException;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.List;

public class BundleSearchService {
    private final IDocumentLoader documentLoader;
    private final BundleSearcherRegistry bundleSearcherRegistry;
    
    public BundleSearchService(IDocumentLoader documentLoader, BundleSearcherRegistry bundleSearcher) {
        this.documentLoader = documentLoader;
        this.bundleSearcherRegistry = bundleSearcher;
    }

    public ResponseDTO searchBundle(QualifiedName bundleId, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException, UnsupportedTargetTypeException {
        StorageCpmDocument retrievedDocument = documentLoader.loadCpmDocument(bundleId.getUri());
        var document = retrievedDocument.document;
        var response = searchDocument(document, startNodeId, targetType, targetSpecification);
        return new ResponseDTO(response.bundleId(), retrievedDocument.token, response.found());
    }

    public ResponseDTO searchDocument(CpmDocument document, QualifiedName startNodeId, ETargetType targetType, JsonNode targetSpecification) throws IOException, UnsupportedTargetTypeException {

        var bundleSearcher = bundleSearcherRegistry.getSearchFunc(targetType);
        
        if (bundleSearcher == null) {
            throw new UnsupportedTargetTypeException("No search function registered for target type: " + targetType);
        }

        Object result = bundleSearcher.apply(document, startNodeId, targetSpecification);

        ResponseDTO responseDTO = new ResponseDTO(
                new QualifiedNameDTO(document.getBundleId()),
                null,
                new ObjectMapper().valueToTree(result));
        
        System.out.println("Response bundleId:\n" + responseDTO.bundleId().toString());
        System.out.println("Response found results:\n" + responseDTO.found());
        
        return responseDTO;
    }
    
    public List<ETargetType> getSupportedTargetTypes() {
        return bundleSearcherRegistry.getAllTargetTypes();
    }
    
}
