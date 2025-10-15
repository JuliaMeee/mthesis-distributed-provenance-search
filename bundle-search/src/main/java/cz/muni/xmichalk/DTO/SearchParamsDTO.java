package cz.muni.xmichalk.DTO;

import org.openprovenance.prov.model.QualifiedName;

public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO connectorId;
    public String targetSpecification;
    public String targetType;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, String targetType, String targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.connectorId = new QualifiedNameDTO().from(connectorId);
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
