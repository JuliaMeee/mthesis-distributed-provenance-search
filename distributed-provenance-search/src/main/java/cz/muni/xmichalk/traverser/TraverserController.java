package cz.muni.xmichalk.traverser;


import cz.muni.xmichalk.dto.FoundResultDTO;
import cz.muni.xmichalk.dto.SearchParamsDTO;
import cz.muni.xmichalk.dto.SearchResultsDTO;
import cz.muni.xmichalk.models.SearchParams;
import cz.muni.xmichalk.models.SearchResults;
import cz.muni.xmichalk.searchPriority.ESearchPriority;
import cz.muni.xmichalk.validity.EValidityCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
public class TraverserController {
    private final Traverser traverser;

    public TraverserController(Traverser traverser) {
        this.traverser = traverser;
    }

    @Operation(summary = "List available validity checks", description = "Returns all defined validity checks.")
    @GetMapping(value = "/api/getValidityChecks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<EValidityCheck>> getAvailableValidityChecks() {
        Set<EValidityCheck> checks = traverser.getValidityVerifiers().keySet();

        return ResponseEntity.ok(checks);
    }

    @Operation(summary = "List available search priority options", description = "Returns all defined search priority options.")
    @GetMapping(value = "/api/getSearchPriorities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<ESearchPriority>> getAvailableSearchPriorities() {
        Set<ESearchPriority> options = traverser.getSearchPriorityComparators().keySet();

        return ResponseEntity.ok(options);
    }

    @Operation(summary = "Search this bundle and its predecessors", description = "Searches for targets fitting the target specification. Starts in the specified bundle and node, and then searches through all its predecessors. Returns all found results.")
    @PostMapping(value = "/api/searchPredecessors", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SearchParamsDTO.class),
                    examples = {
                            @ExampleObject(name = "Find all backward connectors", value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
                                        "localPart": "SpeciesIdentificationBundle_V0"
                                      },
                                      "startNodeId": {
                                        "nameSpaceUri": "https://openprovenance.org/blank/",
                                        "localPart": "IdentifiedSpeciesCon"
                                      },
                                      "versionPreference": "SPECIFIED",
                                      "searchPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "targetType": "CONNECTORS",
                                        "targetSpecification": "backward"
                                    }
                                    """),
                            @ExampleObject(name = "Find all person nodes", value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
                                        "localPart": "SpeciesIdentificationBundle_V0"
                                      },
                                      "startNodeId": {
                                        "nameSpaceUri": "https://openprovenance.org/blank/",
                                        "localPart": "IdentifiedSpeciesCon"
                                      },
                                      "versionPreference": "SPECIFIED",
                                      "searchPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "targetType": "NODES",
                                      "targetSpecification": {
                                        "type" : "HasAttrQualifiedNameValue",
                                        "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                        "uriRegex" : "https://schema.org/Person"
                                      }
                                    
                                    }
                                    """)
                    }
            )
    )
    public ResponseEntity<?> searchPredecessors(
            @RequestBody SearchParamsDTO searchParams) {
        return searchChain(searchParams, true);
    }

    @Operation(summary = "Search this bundle and its successors", description = "Searches for targets fitting the target specification. Starts in the specified bundle and node, and then searches through all its successors. Returns all found results.")
    @PostMapping(value = "/api/searchSuccessors", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SearchParamsDTO.class),
                    examples = {
                            @ExampleObject(name = "Find main activity ids", value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                        "localPart": "SamplingBundle_V0"
                                      },
                                      "startNodeId": {
                                        "nameSpaceUri": "https://openprovenance.org/blank/",
                                        "localPart": "StoredSampleCon_r1"
                                      },
                                      "versionPreference": "LATEST",
                                      "searchPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "targetType": "NODE_IDS",
                                      "targetSpecification": {
                                        "type" : "HasAttrQualifiedNameValue",
                                        "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                        "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/mainActivity"
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "Check if bundles have a backward jump connector to a bundle with defined meta uri", value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                        "localPart": "SamplingBundle_V0"
                                      },
                                      "startNodeId": {
                                        "nameSpaceUri": "https://openprovenance.org/blank/",
                                        "localPart": "StoredSampleCon_r1"
                                      },
                                      "versionPreference": "LATEST",
                                      "searchPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "targetType": "TEST_FITS",
                                      "targetSpecification": {
                                        "type" : "CountCondition",
                                        "findableInDocument" : {
                                          "type" : "FindLinearSubgraphs",
                                          "firstNode" : {
                                            "type" : "AllTrue",
                                            "conditions" : [ {
                                              "type" : "HasAttrQualifiedNameValue",
                                              "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                              "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/backwardConnector"
                                            }, {
                                              "type" : "HasAttrQualifiedNameValue",
                                              "attributeNameUri" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/referencedMetaBundleId",
                                              "uriRegex" : "http://prov-storage-1:8000/api/v1/documents/meta/SamplingBundle_V0_meta"
                                            } ]
                                          },
                                          "edgesAndNodes" : [ {
                                            "isKind" : "PROV_DERIVATION",
                                            "isNotKind" : null,
                                            "nodeIsEffect" : true,
                                            "nodeCondition" : {
                                              "type" : "HasAttrQualifiedNameValue",
                                              "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                              "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/backwardConnector"
                                            }
                                          } ]
                                        },
                                        "comparisonResult" : "GREATER_THAN_OR_EQUALS",
                                        "count" : 1
                                      }
                                    }
                                    """)
                    }
            )
    )
    public ResponseEntity<?> searchSuccessors(
            @RequestBody SearchParamsDTO searchParams) {
        return searchChain(searchParams, false);
    }

    private ResponseEntity<?> searchChain(SearchParamsDTO searchParams, boolean searchBackwards) {
        List<String> missingParams = getMissingParams(searchParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {

            QualifiedName bundleId = searchParams.bundleId.toQN();
            QualifiedName connectorId = searchParams.startNodeId.toQN();

            SearchResults results = traverser.searchChain(
                    bundleId,
                    connectorId,
                    new SearchParams(
                            searchBackwards,
                            searchParams.versionPreference,
                            searchParams.searchPriority != null ? searchParams.searchPriority : ESearchPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                            searchParams.validityChecks != null ? searchParams.validityChecks : new ArrayList<>(),
                            searchParams.targetType,
                            searchParams.targetSpecification
                    )
            );

            SearchResultsDTO resultsDTO = new SearchResultsDTO(
                    results.results.stream()
                            .map(fr -> new FoundResultDTO().from(fr))
                            .toList(),
                    results.errors
            );

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