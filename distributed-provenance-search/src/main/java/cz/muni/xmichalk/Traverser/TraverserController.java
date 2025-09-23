package cz.muni.xmichalk.Traverser;


import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Traverser.Models.SearchBundleResultDTO;
import cz.muni.xmichalk.Traverser.Models.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.Models.SearchResultEntry;
import org.openprovenance.prov.vanilla.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

            System.out.println("Searching predecessors: " + searchParams.bundlePrefixUrl + searchParams.bundleLocalName
                    + " " + searchParams.connectorPrefixUrl + searchParams.connectorLocalName + " " + searchParams.targetSpecification.localName);

            QualifiedName bundleId = new QualifiedName(searchParams.bundlePrefixUrl, searchParams.bundleLocalName, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorPrefixUrl, searchParams.connectorLocalName, null);

            List<SearchResultEntry> results = traverser.searchPredecessors(
                    bundleId,
                    connectorId,
                    searchParams.targetSpecification
            );
            return ResponseEntity.ok(results);
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

            System.out.println("Searching bundle backward: " + searchParams.bundlePrefixUrl + searchParams.bundleLocalName
                    + " " + searchParams.connectorPrefixUrl + searchParams.connectorLocalName + " " + searchParams.targetSpecification.localName);

            QualifiedName bundleId = new QualifiedName(searchParams.bundlePrefixUrl, searchParams.bundleLocalName, null);
            QualifiedName connectorId = new QualifiedName(searchParams.connectorPrefixUrl, searchParams.connectorLocalName, null);
            Predicate<INode> targetPredicate = translateToPredicate(searchParams.targetSpecification);

            SearchBundleResultDTO searchBundleResult = traverser.searchBundleBackward(bundleId, connectorId, targetPredicate);

            return ResponseEntity.ok(searchBundleResult);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}