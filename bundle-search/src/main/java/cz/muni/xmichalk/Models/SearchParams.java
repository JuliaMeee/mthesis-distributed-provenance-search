package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.BundleSearch.ETargetType;
import io.swagger.v3.oas.annotations.media.Schema;

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
                  "targetType": "CONNECTORS",
                  "targetSpecification": "backward"
                }
                """
)
public class SearchParams {
    public QualifiedNameData bundleId;
    public QualifiedNameData startNodeId;
    public ETargetType targetType;
    public JsonNode targetSpecification;

    public SearchParams() {

    }

    public SearchParams(org.openprovenance.prov.model.QualifiedName bundleId, org.openprovenance.prov.model.QualifiedName connectorId, ETargetType targetType, JsonNode targetSpecification) {
        this.bundleId = new QualifiedNameData(bundleId);
        this.startNodeId = new QualifiedNameData(connectorId);
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
