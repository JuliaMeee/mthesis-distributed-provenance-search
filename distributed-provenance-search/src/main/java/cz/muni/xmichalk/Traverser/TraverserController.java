package cz.muni.xmichalk.Traverser;


import cz.muni.xmichalk.Traverser.DTO.FoundResultDTO;
import cz.muni.xmichalk.Traverser.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.Models.FoundResult;
import cz.muni.xmichalk.Traverser.Models.SearchParams;
import io.swagger.v3.oas.annotations.Operation;
import org.openprovenance.prov.vanilla.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TraverserController {
    private final Traverser traverser;

    public TraverserController(Traverser traverser) {
        this.traverser = traverser;
    }

    @Operation(summary = "Search this bundle and its predecessors", description = "Searches for targets fitting the target specification. Starts in the specified bundle and node, and then searches through all its predecessors. Returns all found results.")
    @PostMapping(value = "/api/searchPredecessors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchPredecessors(
            @RequestBody SearchParamsDTO searchParams) {
        return searchChain(searchParams, true);
    }

    @Operation(summary = "Search this bundle and its successors", description = "Searches for targets fitting the target specification. Starts in the specified bundle and node, and then searches through all its successors. Returns all found results.")
    @PostMapping(value = "/api/searchSuccessors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchSuccessors(
            @RequestBody SearchParamsDTO searchParams) {
        return searchChain(searchParams, false);
    }

    private ResponseEntity searchChain(SearchParamsDTO searchParams, boolean searchBackwards) {
        if (isMissingRequiredParams(searchParams)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields in the request body.");
        }

        try {

            System.out.println("Searching " + (searchBackwards ? "predecessors" : "successors") + ": " + searchParams.bundleId.nameSpaceUri + searchParams.bundleId.localPart
                    + " " + searchParams.startNodeId.nameSpaceUri + searchParams.startNodeId.localPart + " " + searchParams.targetSpecification);

            QualifiedName bundleId = new QualifiedName(searchParams.bundleId.nameSpaceUri, searchParams.bundleId.localPart, null);
            QualifiedName connectorId = new QualifiedName(searchParams.startNodeId.nameSpaceUri, searchParams.startNodeId.localPart, null);

            List<FoundResult> results = traverser.searchChain(
                    bundleId,
                    connectorId,
                    new SearchParams(
                            searchBackwards,
                            searchParams.targetType,
                            searchParams.targetSpecification
                    )
            );

            List<FoundResultDTO> resultsDTO = results.stream()
                    .map(fr -> new FoundResultDTO().from(fr))
                    .toList();

            return ResponseEntity.ok(resultsDTO);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    private static boolean isMissingRequiredParams(SearchParamsDTO params) {
        return params.bundleId == null || params.startNodeId == null || params.targetType == null || params.targetSpecification == null;
    }
}