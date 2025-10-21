package cz.muni.xmichalk;

import cz.muni.xmichalk.BundleSearch.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleSearch.ETargetType;
import cz.muni.xmichalk.DTO.ResponseDTO;
import cz.muni.xmichalk.DTO.SearchParamsDTO;
import cz.muni.xmichalk.DTO.TargetTypeInfoDTO;
import cz.muni.xmichalk.Util.Pair;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.SerializationUtils.deserialize;

@RestController
public class BundleSearchController {

    private final BundleSearchService bundleSearchService;

    public BundleSearchController(BundleSearchService bundleSearchService) {
        this.bundleSearchService = bundleSearchService;
    }

    @Operation(summary = "Get all available target types", description = "Returns all defined target types and their descriptions.")
    @GetMapping(value = "/api/getTargetTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TargetTypeInfoDTO>> getAvailableSearchTypes() {
        var types = BundleSearcherRegistry.getAllTargetTypes().stream().map(t -> new TargetTypeInfoDTO(t, t.description));
        
        return ResponseEntity.ok(types.collect(Collectors.toList()));
    }

    @Operation(summary = "Search bundle in backward direction", description = "Search bundle in the backward direction starting from the specified connector.")
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
