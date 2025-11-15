package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.bundleSearch.ETargetType;
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

    public SearchParams(QualifiedNameData bundleId, QualifiedNameData startNodeId, ETargetType targetType, JsonNode targetSpecification) {
        this.bundleId = bundleId;
        this.startNodeId = startNodeId;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}