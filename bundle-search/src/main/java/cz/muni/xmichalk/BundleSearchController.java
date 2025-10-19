package cz.muni.xmichalk;

import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

import static org.apache.commons.lang3.SerializationUtils.deserialize;

@RestController
public class BundleSearchController {

    private final BundleSearchService bundleSearchService;

    public BundleSearchController(BundleSearchService bundleSearchService) {
        this.bundleSearchService = bundleSearchService;
    }

    @GetMapping(value = "/api/helloWorld", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> helloWorld() {

        return ResponseEntity.ok("HelloWorld");
    }

    @PostMapping(value = "/api/searchBundleBackward", produces = MediaType.APPLICATION_JSON_VALUE)
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
            
            return ResponseEntity.ok(searchBundleResult);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    
    @PostMapping(value = "/api/testSearch", produces = MediaType.APPLICATION_JSON_VALUE)
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
            
            var document = ProvDocumentUtils.deserializeFile(Path.of(System.getProperty("user.dir") + "/src/main/resources/data/dataset3/SpeciesIdentificationBundle_V0.json"), Formats.ProvFormat.JSON);
            
            var searchBundleResult = bundleSearchService.searchBundleBackward(document, connectorId, searchParams.targetType, searchParams.targetSpecification);
            
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
