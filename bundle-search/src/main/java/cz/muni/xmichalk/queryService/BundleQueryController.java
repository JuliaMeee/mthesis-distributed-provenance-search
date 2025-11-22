package cz.muni.xmichalk.queryService;

import cz.muni.xmichalk.bundleVersionPicker.EVersionPreferrence;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.models.PickVersionParams;
import cz.muni.xmichalk.models.QualifiedNameData;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController
public class BundleQueryController {

    private static final Logger log = LoggerFactory.getLogger(BundleQueryController.class);
    private final BundleQueryService bundleQueryService;
    private final Map<EVersionPreferrence, IVersionPicker> versionPickers;

    public BundleQueryController(BundleQueryService bundleQueryService, Map<EVersionPreferrence, IVersionPicker> versionPickers) {
        this.bundleQueryService = bundleQueryService;
        this.versionPickers = versionPickers;
    }

    @Operation(summary = "List available version preferences", description = "Returns all defined version preferences.")
    @GetMapping(value = "/api/getVersionPreferences", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<EVersionPreferrence>> getAvailableVersionPreferences() {
        Set<EVersionPreferrence> preferences = versionPickers.keySet();

        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Chooses bundle version based on set preference", description = "Based on supplied bundle id and version preference returns the preferred bundle version id.")
    @PostMapping(value = "/api/pickVersion", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Pick Version Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = PickVersionParams.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
                                        "localPart": "SamplingBundle_V0"
                                      },
                                      "versionPreference": "LATEST"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<?> pickVersion(@RequestBody PickVersionParams params) {
        List<String> missingParams = getMissingParams(params);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {
            IVersionPicker versionPicker = versionPickers.get(params.versionPreference);

            if (versionPicker == null) {
                String errorMessage = "Unsupported version preference: " + params.versionPreference;
                log.error(errorMessage);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(errorMessage);
            }

            QualifiedName pickedBundleVersion = versionPicker.apply(params.bundleId.toQN());

            return ResponseEntity.ok(new QualifiedNameData().from(pickedBundleVersion));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
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
                                                  "nodePredicate" : {
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
                                                    "nodePredicate" : {
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

            QueryResult queryResult = bundleQueryService.evaluateBundleQuery(bundleId, connectorId, queryParams.querySpecification);
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

    private static List<String> getMissingParams(PickVersionParams params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.versionPreference == null) missing.add("versionPreference");

        return missing;
    }
}
