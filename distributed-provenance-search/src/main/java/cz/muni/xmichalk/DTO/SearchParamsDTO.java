package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openprovenance.prov.model.QualifiedName;

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
                  "bundleRelevanceRequirements": {...},
                  "targetType": "CONNECTORS",
                  "targetSpecification": "backward"
                }
                """
)
public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO startNodeId;
    public String versionPreference;
    public JsonNode bundleRelevanceRequirements;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, String versionPreference, JsonNode bundleRelevanceRequirements, String targetType, JsonNode targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.startNodeId = new QualifiedNameDTO().from(connectorId);
        this.bundleRelevanceRequirements = bundleRelevanceRequirements;
        this.versionPreference = versionPreference;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
