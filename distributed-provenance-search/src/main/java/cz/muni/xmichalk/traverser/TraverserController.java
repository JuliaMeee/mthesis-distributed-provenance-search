package cz.muni.xmichalk.traverser;


import cz.muni.xmichalk.dto.FoundResultDTO;
import cz.muni.xmichalk.dto.TraversalParamsDTO;
import cz.muni.xmichalk.dto.TraversalResultsDTO;
import cz.muni.xmichalk.models.TraversalParams;
import cz.muni.xmichalk.models.TraversalResults;
import cz.muni.xmichalk.traversalPriority.ETraversalPriority;
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

    @Operation(summary = "List available traversal priority options", description = "Returns all defined traversal priority options.")
    @GetMapping(value = "/api/getTraversalPriorities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<ETraversalPriority>> getAvailableTraversalPriorities() {
        Set<ETraversalPriority> options = traverser.getTraversalPriorityComparators().keySet();

        return ResponseEntity.ok(options);
    }

    @Operation(summary = "Traverses given bundle and its predecessors to find results for the query.")
    @PostMapping(value = "/api/traversePredecessors", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Traversal Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TraversalParamsDTO.class),
                    examples = {
                            @ExampleObject(name = "Get all person nodes", value = """
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
                                      "traversalPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "querySpecification": {
                                        "type": "GetNodes",
                                        "nodeFinder": {
                                          "type" : "FindFittingNodes",
                                          "nodeCondition" : {
                                            "type" : "HasAttrQualifiedNameValue",
                                            "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                            "uriRegex" : "https://schema.org/Person"
                                          }
                                        }
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "Get all backward connectors", value = """
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
                                      "traversalPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "querySpecification": {
                                        "type": "GetConnectors",
                                        "backward": "true"
                                      }
                                    }
                                    """)

                    }
            )
    )
    public ResponseEntity<?> traversePredecessors(
            @RequestBody TraversalParamsDTO traverseParams) {
        return traverseChain(traverseParams, true);
    }

    @Operation(summary = "Traverses given bundle and its successors to find results for the query.")
    @PostMapping(value = "/api/traverseSuccessors", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Traversal Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TraversalParamsDTO.class),
                    examples = {
                            @ExampleObject(name = "Get main activity ids", value = """
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
                                      "traversalPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "querySpecification": {
                                        "type": "GetNodeIds",
                                        "nodeFinder": {
                                          "type" : "FindFittingNodes",
                                          "nodeCondition" : {
                                            "type" : "HasAttrQualifiedNameValue",
                                            "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                            "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/mainActivity"
                                          }
                                        }
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
                                      "traversalPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                                      "validityChecks": ["DEMO_SIMPLE_CONSTRAINTS"],
                                      "querySpecification": {
                                        "type": "TestBundleFits",
                                        "condition": {
                                          "type" : "CountCondition",
                                          "findableInDocument" : {
                                            "type" : "FindFittingLinearSubgraphs",
                                            "graphParts" : [ {
                                              "type" : "EdgeToNodeCondition",
                                              "nodeCondition" : {
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
                                              }
                                            }, {
                                              "type" : "EdgeToNodeCondition",
                                              "edgeCondition" : {
                                                "type" : "IsRelation",
                                                "relation" : "PROV_DERIVATION"
                                              },
                                              "nodeCondition" : {
                                                "type" : "HasAttrQualifiedNameValue",
                                                "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                                "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/backwardConnector"
                                              },
                                              "nodeIsEffect" : true
                                            } ]
                                          },
                                          "comparisonResult" : "GREATER_THAN_OR_EQUALS",
                                          "count" : 1
                                        }
                                      }
                                    }
                                    """)
                    }
            )
    )
    public ResponseEntity<?> traverseSuccessors(
            @RequestBody TraversalParamsDTO traversalParams) {
        return traverseChain(traversalParams, false);
    }

    private ResponseEntity<?> traverseChain(TraversalParamsDTO traversalParams, boolean traverseBackwards) {
        List<String> missingParams = getMissingParams(traversalParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {

            QualifiedName bundleId = traversalParams.bundleId.toQN();
            QualifiedName connectorId = traversalParams.startNodeId.toQN();

            TraversalResults results = traverser.traverseChain(
                    bundleId,
                    connectorId,
                    new TraversalParams(
                            traverseBackwards,
                            traversalParams.versionPreference,
                            traversalParams.traversalPriority != null ? traversalParams.traversalPriority : ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                            traversalParams.validityChecks != null ? traversalParams.validityChecks : new ArrayList<>(),
                            traversalParams.querySpecification
                    )
            );

            TraversalResultsDTO resultsDTO = new TraversalResultsDTO(
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

    private static List<String> getMissingParams(TraversalParamsDTO params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.startNodeId == null) missing.add("startNodeId");
        if (params.querySpecification == null) missing.add("querySpecification");

        return missing;
    }
}