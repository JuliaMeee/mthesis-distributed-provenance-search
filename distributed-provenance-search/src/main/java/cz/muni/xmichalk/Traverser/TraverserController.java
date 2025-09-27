package cz.muni.xmichalk.Traverser;


import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Traverser.DTO.SearchBundleResultDTO;
import cz.muni.xmichalk.Traverser.DTO.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.DTO.SearchResultsDTO;
import cz.muni.xmichalk.Traverser.Models.SearchBundleResult;
import cz.muni.xmichalk.Traverser.Models.SearchResults;
import org.openprovenance.prov.vanilla.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Predicate;

import static cz.muni.xmichalk.Traverser.TraverserUtils.isMissingRequiredParams;
import static cz.muni.xmichalk.Traverser.TraverserUtils.translateToPredicate;

@RestController
public class TraverserController {
    private final Traverser traverser;

    public TraverserController(Traverser traverser) {
        this.traverser = traverser;
    }

    @GetMapping("/api/helloWorld")
    public ResponseEntity<?> helloWorld() {

        return ResponseEntity.ok("HelloWorld");
    }

    @PostMapping("/api/searchPredecessors")
    public ResponseEntity<?> searchPredecessors(
            @RequestBody SearchParamsDTO searchParams) {
        if (isMissingRequiredParams(searchParams)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields in the request body.");
        }

        try {

            System.out.println("Searching predecessors: " + searchParams.bundleId.nameSpaceUri + searchParams.bundleId.localPart
                    + " " + searchParams.connectorId.nameSpaceUri + searchParams.connectorId.localPart + " " + searchParams.targetSpecification.localName);

            QualifiedName bundleId = new QualifiedName(searchParams.bundleId.nameSpaceUri, searchParams.bundleId.localPart, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorId.nameSpaceUri, searchParams.connectorId.localPart, null);

            SearchResults results = traverser.searchPredecessors(
                    bundleId,
                    connectorId,
                    searchParams.targetSpecification
            );

            SearchResultsDTO resultsDTO = new SearchResultsDTO().from(results);

            return ResponseEntity.ok(resultsDTO);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
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
                    + " " + searchParams.connectorId.nameSpaceUri + searchParams.connectorId.localPart + " " + searchParams.targetSpecification.localName);

            QualifiedName bundleId = new QualifiedName(searchParams.bundleId.nameSpaceUri, searchParams.bundleId.localPart, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorId.nameSpaceUri, searchParams.connectorId.localPart, null);
            Predicate<INode> targetPredicate = translateToPredicate(searchParams.targetSpecification);

            SearchBundleResult searchBundleResult = traverser.searchBundleBackward(bundleId, connectorId, targetPredicate);
            SearchBundleResultDTO searchBundleResultDTO = new SearchBundleResultDTO().from(searchBundleResult);

            return ResponseEntity.ok(searchBundleResultDTO);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}