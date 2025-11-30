package cz.muni.xmichalk.queryService;

import cz.muni.xmichalk.models.QueryParams;
import cz.muni.xmichalk.models.QueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class BundleQueryController {

    private static final Logger log = LoggerFactory.getLogger(BundleQueryController.class);
    private final BundleQueryService bundleQueryService;

    public BundleQueryController(BundleQueryService bundleQueryService) {
        this.bundleQueryService = bundleQueryService;
    }

    @Operation(summary = "Answer a given query about a given bundle", description = "Traverse the bundle from the specified node to answer the query. Can also use the metadata of the bundle to answer the query.")
    @PostMapping(value = "/api/bundleQuery", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Query Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = QueryParams.class),
                    examples = {
                            @ExampleObject(
                                    name = "Get all person nodes in the bundle",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type" : "GetNodes",
                                                "nodeFinder" : {
                                                  "type" : "FindFittingNodes",
                                                  "nodeCondition" : {
                                                    "type" : "HasAttrQualifiedNameValue",
                                                    "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                                    "uriRegex" : "https://schema.org/Person"
                                                  }
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get subgraph formed by only Derivation and Specialization relations going in both directions from the start node",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type" : "GetFilteredSubgraph",
                                                "pathCondition": {
                                                  "type": "DerivationPathCondition",
                                                  "backward": null
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get all mainActivity and connector nodes on path formed by Derivation, Usage or Generation relations going in backward direction from the start node",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                 "type" : "GetNodes",
                                                 "nodeFinder" : {
                                                   "type" : "FindFittingNodes",
                                                   "nodeCondition" : null,
                                                   "pathCondition" : {
                                                     "type" : "EdgeToNodeCondition",
                                                     "edgeCondition" : {
                                                       "type" : "AnyTrue",
                                                       "conditions" : [ {
                                                         "type" : "IsRelation",
                                                         "relation" : "PROV_DERIVATION"
                                                       }, {
                                                         "type" : "IsRelation",
                                                         "relation" : "PROV_USAGE"
                                                       }, {
                                                         "type" : "IsRelation",
                                                         "relation" : "PROV_GENERATION"
                                                       } ]
                                                     },
                                                     "nodeCondition" : null,
                                                     "nodeIsEffect" : null
                                                   }
                                                 }
                                               }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get all forward connectors in the bundle",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type" : "GetConnectors",
                                                "backward" : false
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get forward connectors derived from a specific connector",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type" : "GetConnectors",
                                                "backward" : false,
                                                "pathCondition": {
                                                  "type": "DerivationPathCondition",
                                                  "backward": false
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Test if bundle has exactly one main activity",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type": "TestBundleFits",
                                                "condition": {
                                                  "type" : "CountCondition",
                                                  "findableInDocument" : {
                                                    "type" : "FindFittingNodes",
                                                    "nodeCondition" : {
                                                      "type" : "HasAttrQualifiedNameValue",
                                                      "attributeNameUri" : "http://www.w3.org/ns/prov#type",
                                                      "uriRegex" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/mainActivity"
                                                    }
                                                  },
                                                  "comparisonResult" : "EQUALS",
                                                  "count" : 1
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get all activities Jane Smith was responsible for",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V1"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type": "GetSubgraphs",
                                                "subgraphFinder": {
                                                  "type": "FindFittingLinearSubgraphs",
                                                  "graphParts": [
                                                    {
                                                      "type": "EdgeToNodeCondition",
                                                      "edgeCondition": null,
                                                      "nodeCondition": {
                                                        "type": "AllTrue",
                                                        "conditions": [
                                                          {
                                                            "type": "HasAttrQualifiedNameValue",
                                                            "attributeNameUri": "http://www.w3.org/ns/prov#type",
                                                            "uriRegex": "https://schema.org/Person"
                                                          },
                                                          {
                                                            "type": "HasAttrLangStringValue",
                                                            "attributeNameUri": "https://schema.org/name",
                                                            "langRegex": null,
                                                            "valueRegex": "Jane Smith"
                                                          }
                                                        ]
                                                      },
                                                      "nodeIsEffect": null
                                                    },
                                                    {
                                                      "type": "EdgeToNodeCondition",
                                                      "edgeCondition": {
                                                        "type": "IsRelation",
                                                        "relation": "PROV_ASSOCIATION"
                                                      },
                                                      "nodeCondition": {
                                                        "type": "IsKind",
                                                        "kind": "PROV_ACTIVITY"
                                                      },
                                                      "nodeIsEffect": null
                                                    }
                                                  ]
                                                }
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Get bundle id of the newest version of this bundle",
                                    value = """
                                            {
                                              "bundleId": {
                                                "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                                "localPart": "SamplingBundle_V0"
                                              },
                                              "startNodeId": {
                                                "nameSpaceUri": "https://openprovenance.org/blank/",
                                                "localPart": "StoredSampleCon_r1"
                                              },
                                              "querySpecification": {
                                                "type" : "GetPreferredVersion",
                                                "versionPreference" : "LATEST"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<?> bundleQuery(
            @RequestBody QueryParams queryParams) {

        List<String> missingParams = getMissingParams(queryParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {
            QualifiedName bundleId = queryParams.bundleId.toQN();
            QualifiedName connectorId = queryParams.startNodeId.toQN();

            log.info("Received bundle query request for bundleId: {}, startNodeId: {}, query type: {}", bundleId, connectorId, queryParams.querySpecification.getClass().getName());

            QueryResult queryResult = bundleQueryService.evaluateBundleQuery(bundleId, connectorId, queryParams.querySpecification);

            log.info("Answering bundle query request for bundleId: {}, startNodeId: {}, query type: {}, with result: {}", bundleId, connectorId, queryParams.querySpecification.getClass().getName(), queryResult.result);
            return ResponseEntity.ok(queryResult);
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

    private static List<String> getMissingParams(QueryParams params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.startNodeId == null) missing.add("startNodeId");
        if (params.querySpecification == null) missing.add("querySpecification");

        return missing;
    }
}
