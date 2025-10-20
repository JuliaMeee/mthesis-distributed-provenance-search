package cz.muni.xmichalk.DTO;

import cz.muni.xmichalk.BundleSearch.ETargetType;
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
    public ETargetType targetType;
    public String targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, ETargetType targetType, String targetSpecification) {
        this.bundleId = new QualifiedNameDTO(bundleId);
        this.connectorId = new QualifiedNameDTO(connectorId);
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
