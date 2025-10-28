package cz.muni.xmichalk.Traverser;


import cz.muni.xmichalk.DTO.FoundResultDTO;
import cz.muni.xmichalk.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Models.FoundResult;
import cz.muni.xmichalk.Models.SearchParams;
import io.swagger.v3.oas.annotations.Operation;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
        List<String> missingParams = getMissingParams(searchParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {

            QualifiedName bundleId = searchParams.bundleId.toDomainModel();
            QualifiedName connectorId = searchParams.startNodeId.toDomainModel();

            List<FoundResult> results = traverser.searchChain(
                    bundleId,
                    connectorId,
                    new SearchParams(
                            searchBackwards,
                            searchParams.versionPreference,
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

    private static String getMissingParamsMessage(List<String> missingParams) {
        StringBuilder builder = new StringBuilder("Missing required fields in the request body: ");
        for (int i = 0; i < missingParams.size(); i++) {
            builder.append(missingParams.get(i));
            if (i < missingParams.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(".");
        return builder.toString();
    }

    private static List<String> getMissingParams(SearchParamsDTO params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.startNodeId == null) missing.add("startNodeId");
        if (params.targetType == null) missing.add("targetType");
        if (params.targetSpecification == null) missing.add("targetSpecification");

        return missing;
    }
}