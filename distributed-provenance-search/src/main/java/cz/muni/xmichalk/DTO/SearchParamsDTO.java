package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.SearchPriority.ESearchPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;

@Schema(
        description = "Parameters for bundle search",
        example = """
                {
                  "bundleId": {
                    "nameSpaceUri": "http://example.org/",
                    "localPart": "bundle1"
                  },
                  "startNodeId": {
                    "nameSpaceUri": "http://example.org/",
                    "localPart": "connectorA"
                  },
                  "versionPreference": "LATEST",
                  "searchPriority": "INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS",
                  "validityChecks": {SIMPLE_DEMO},
                  "targetType": "CONNECTORS",
                  "targetSpecification": "backward"
                }
                """
)
public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO startNodeId;
    public String versionPreference;
    public ESearchPriority searchPriority;
    public List<EValiditySpecification> validityChecks;
    public JsonNode bundleRelevanceRequirements;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, String versionPreference, ESearchPriority searchPriority, List<EValiditySpecification> validityChecks, JsonNode bundleRelevanceRequirements, String targetType, JsonNode targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.startNodeId = new QualifiedNameDTO().from(connectorId);
        this.bundleRelevanceRequirements = bundleRelevanceRequirements;
        this.versionPreference = versionPreference;
        this.searchPriority = searchPriority;
        this.validityChecks = validityChecks;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
