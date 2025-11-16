package cz.muni.xmichalk.bundleSearcherService;

import cz.muni.xmichalk.bundleSearch.UnsupportedTargetTypeException;
import cz.muni.xmichalk.bundleVersionPicker.EVersionPreferrence;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.models.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
public class BundleSearchController {

    private static final Logger log = LoggerFactory.getLogger(BundleSearchController.class);
    private final BundleSearchService bundleSearchService;
    private final Map<EVersionPreferrence, IVersionPicker> versionPickers;

    public BundleSearchController(BundleSearchService bundleSearchService, Map<EVersionPreferrence, IVersionPicker> versionPickers) {
        this.bundleSearchService = bundleSearchService;
        this.versionPickers = versionPickers;
    }

    @Operation(summary = "List available target types", description = "Returns all defined target types and their descriptions.")
    @GetMapping(value = "/api/getTargetTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TargetTypeInfo>> getAvailableSearchTypes() {
        Stream<TargetTypeInfo> types = bundleSearchService.getBundleSearchers().keySet().stream().map(t -> new TargetTypeInfo(t, t.description));

        return ResponseEntity.ok(types.collect(Collectors.toList()));
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

    @Operation(summary = "Search bundle looking for given target", description = "Search bundle for given target starting from the specified node.")
    @PostMapping(value = "/api/searchBundle", produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search Params",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SearchParams.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "bundleId": {
                                        "nameSpaceUri": "http://prov-storage-2:8000/api/v1/organizations/ORG2/documents/",
                                        "localPart": "ProcessingBundle_V1"
                                      },
                                      "startNodeId": {
                                        "nameSpaceUri": "https://openprovenance.org/blank/",
                                        "localPart": "StoredSampleCon_r1"
                                      },
                                      "targetType": "NODES_BY_SPECIFICATION",
                                      "targetSpecification": {
                                        "type": "NodeSpecification",
                                        "hasAttributeValues": [
                                          {
                                            "type": "QualifiedNameAttrSpecification",
                                            "attributeNameUri": "http://www.w3.org/ns/prov#type",
                                            "uriRegex": "(?i).*person"
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<?> searchBundle(
            @RequestBody SearchParams searchParams) {

        List<String> missingParams = getMissingParams(searchParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {
            QualifiedName bundleId = searchParams.bundleId.toQN();
            QualifiedName connectorId = searchParams.startNodeId.toQN();

            SearchResult searchBundleResult = bundleSearchService.searchBundle(bundleId, connectorId, searchParams.targetType, searchParams.targetSpecification);
            return ResponseEntity.ok(searchBundleResult);

        } catch (UnsupportedTargetTypeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
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

    private static List<String> getMissingParams(SearchParams params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.startNodeId == null) missing.add("startNodeId");
        if (params.targetType == null) missing.add("targetType");
        if (params.targetSpecification == null) missing.add("targetSpecification");

        return missing;
    }

    private static List<String> getMissingParams(PickVersionParams params) {
        List<String> missing = new ArrayList<String>();
        if (params.bundleId == null) missing.add("bundleId");
        if (params.versionPreference == null) missing.add("versionPreference");

        return missing;
    }
}
