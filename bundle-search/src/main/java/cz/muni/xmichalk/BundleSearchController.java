package cz.muni.xmichalk;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleVersionPicker.EVersionPreferrence;
import cz.muni.xmichalk.BundleVersionPicker.VersionPickerRegistry;
import cz.muni.xmichalk.Exceptions.UnsupportedTargetTypeException;
import cz.muni.xmichalk.Exceptions.UnsupportedVersionPreferrenceException;
import cz.muni.xmichalk.Models.*;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
public class BundleSearchController {

    private final BundleSearchService bundleSearchService;
    private final VersionPickerRegistry versionPickerRegistry;

    public BundleSearchController(BundleSearchService bundleSearchService, VersionPickerRegistry versionPickerRegistry) {
        this.bundleSearchService = bundleSearchService;
        this.versionPickerRegistry = versionPickerRegistry;
    }

    @Operation(summary = "List available target types", description = "Returns all defined target types and their descriptions.")
    @GetMapping(value = "/api/getTargetTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TargetTypeInfo>> getAvailableSearchTypes() {
        var types = bundleSearchService.getSupportedTargetTypes().stream().map(t -> new TargetTypeInfo(t, t.description));

        return ResponseEntity.ok(types.collect(Collectors.toList()));
    }

    @Operation(summary = "List available version preferences", description = "Returns all defined version preferences.")
    @GetMapping(value = "/api/getVersionPreferences", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EVersionPreferrence>> getAvailableVersionPreferences() {
        var preferences = versionPickerRegistry.getAllVersionPreferrences();

        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Chooses bundle version based on set preference", description = "Based on supplied bundle id and version preference returns the preferred bundle version id.")
    @PostMapping(value = "/api/pickVersion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pickVersion(@RequestBody PickVersionParams params) {
        try {
            var picker = versionPickerRegistry.getVersionPicker(params.versionPreference());
            var pickedBundleVersion = picker.apply(params.bundleId().toQN());

            return ResponseEntity.ok(new QualifiedNameData(pickedBundleVersion));
        } catch (UnsupportedVersionPreferrenceException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @Operation(summary = "Search bundle looking for given target", description = "Search bundle for given target starting from the specified node.")
    @PostMapping(value = "/api/searchBundle", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/api/testSearch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchTest(
            @RequestBody SearchParams searchParams) {

        List<String> missingParams = getMissingParams(searchParams);
        if (!missingParams.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getMissingParamsMessage(missingParams));
        }

        try {

            System.out.println("Searching bundle backward: " + searchParams.bundleId.nameSpaceUri + searchParams.bundleId.localPart
                    + " " + searchParams.startNodeId.nameSpaceUri + searchParams.startNodeId.localPart + " " + searchParams.targetType + " " + searchParams.targetSpecification);

            QualifiedName bundleId = searchParams.bundleId.toQN();
            QualifiedName connectorId = searchParams.startNodeId.toQN();

            // var document = ProvDocumentUtils.deserializeFile(Path.of(System.getProperty("user.dir") + "/src/main/resources/data/dataset3/SpeciesIdentificationBundle_V0.json"), Formats.ProvFormat.JSON);
            // var document = ProvDocumentUtils.deserializeFile(Path.of(System.getProperty("user.dir") + "/src/main/resources/data/dataset2/ProcessingBundle_V0.json"), Formats.ProvFormat.JSON);
            var document = ProvDocumentUtils.deserializeFile(Path.of(System.getProperty("user.dir") + "/src/main/resources/data/dataset1/SamplingBundle_V1.json"), Formats.ProvFormat.JSON);
            var cpmDocument = new CpmDocument(document, new ProvFactory(), new CpmProvFactory(), new CpmMergedFactory());

            var searchBundleResult = bundleSearchService.searchDocument(cpmDocument, connectorId, searchParams.targetType, searchParams.targetSpecification);

            return ResponseEntity.ok(searchBundleResult);
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
}
