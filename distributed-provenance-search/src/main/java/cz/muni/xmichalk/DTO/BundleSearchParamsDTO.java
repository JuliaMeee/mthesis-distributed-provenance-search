package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import org.openprovenance.prov.model.QualifiedName;

public class BundleSearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO startNodeId;
    public String targetType;
    public JsonNode targetSpecification;

    public BundleSearchParamsDTO() {

    }

    public BundleSearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, String targetType, JsonNode targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.startNodeId = new QualifiedNameDTO().from(connectorId);
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
