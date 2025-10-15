package cz.muni.xmichalk;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearcher.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearcher.IBundleSearcher;
import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DTO.SearchParamsDTO;
import cz.muni.xmichalk.DocumentLoader.DocumentWithIntegrity;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import cz.muni.xmichalk.Util.ProvJsonUtils;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.openprovenance.prov.vanilla.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.PublicKey;
import java.text.Normalizer;
import java.util.function.Predicate;

import static org.apache.commons.lang3.SerializationUtils.deserialize;

@RestController
public class BundleSearchController {

    private final BundleSearchService bundleSearchService;

    public BundleSearchController(BundleSearchService bundleSearchService) {
        this.bundleSearchService = bundleSearchService;
    }

    @GetMapping("/api/helloWorld")
    public ResponseEntity<?> helloWorld() {

        return ResponseEntity.ok("HelloWorld");
    }

    @PostMapping("/api/searchBundleBackward")
    public ResponseEntity<?> searchBundleBackward(
            @RequestBody SearchParamsDTO searchParams) {

        if (isMissingRequiredParams(searchParams)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields in the request body.");
        }

        try {

            System.out.println("Searching bundle backward: " + searchParams.bundleId.nameSpaceUri + searchParams.bundleId.localPart
                    + " " + searchParams.connectorId.nameSpaceUri + searchParams.connectorId.localPart + " " + searchParams.targetType + " " + searchParams.targetSpecification);

            QualifiedName bundleId = new QualifiedName(searchParams.bundleId.nameSpaceUri, searchParams.bundleId.localPart, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorId.nameSpaceUri, searchParams.connectorId.localPart, null);
            
            ResponseDTO searchBundleResult = bundleSearchService.searchBundleBackward(bundleId, connectorId, searchParams.targetType, searchParams.targetSpecification);

            System.out.println("Response found:\n" + searchBundleResult.found);
            System.out.println("Response connectors:\n" + searchBundleResult.connectors);


            return ResponseEntity.ok(searchBundleResult);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    
    @PostMapping("/api/testSearch")
    public ResponseEntity<?> searchTest(
            @RequestBody SearchParamsDTO searchParams) {

        if (isMissingRequiredParams(searchParams)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields in the request body.");
        }

        try {

            System.out.println("Searching bundle backward: " + searchParams.bundleId.nameSpaceUri + searchParams.bundleId.localPart
                    + " " + searchParams.connectorId.nameSpaceUri + searchParams.connectorId.localPart + " " + searchParams.targetType + " " + searchParams.targetSpecification);

            QualifiedName bundleId = new QualifiedName(searchParams.bundleId.nameSpaceUri, searchParams.bundleId.localPart, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorId.nameSpaceUri, searchParams.connectorId.localPart, null);
            
            org.openprovenance.prov.vanilla.ProvFactory pF = new ProvFactory();
            ICpmFactory cF = new CpmMergedFactory(pF);
            ICpmProvFactory cPF = new CpmProvFactory(pF);
            
            var document = ProvDocumentUtils.deserializeFile(Path.of(System.getProperty("user.dir") + "/src/main/resources/data/dataset3/SpeciesIdentificationBundle_V0.json"), Formats.ProvFormat.JSON);
            
            var searchBundleResult = bundleSearchService.searchBundleBackward(document, connectorId, searchParams.targetType, searchParams.targetSpecification);

            System.out.println("Response found:\n" + searchBundleResult.found);
            System.out.println("Response connectors:\n" + searchBundleResult.connectors);

            // Document deserializedConnectorsDoc = ProvDocumentUtils.deserialize(searchBundleResult.connectors, Formats.ProvFormat.JSON);
            
            return ResponseEntity.ok(searchBundleResult);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
    
    private static boolean isMissingRequiredParams(SearchParamsDTO params) {
        return params.bundleId == null || params.connectorId == null || params.targetType == null || params.targetSpecification == null;
    }
}
