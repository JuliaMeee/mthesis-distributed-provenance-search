package cz.muni.xmichalk.Traverser.DTO;

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
                  "connectorId": {
                    "nameSpaceUri": "http://example.org/",
                    "localPart": "connectorA"
                  },
                  "targetType": "CONNECTORS",
                  "targetSpecification": "backward"
                }
                """
)
public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO connectorId;
    public JsonNode targetSpecification;
    public String targetType;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, String targetType, JsonNode targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.connectorId = new QualifiedNameDTO().from(connectorId);
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
