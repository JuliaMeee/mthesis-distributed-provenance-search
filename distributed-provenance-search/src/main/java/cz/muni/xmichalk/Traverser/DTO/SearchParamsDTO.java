package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.TargetSpecification;
import org.openprovenance.prov.model.QualifiedName;

public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO connectorId;
    public TargetSpecification targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, TargetSpecification targetSpecification) {
        this.bundleId = new QualifiedNameDTO().from(bundleId);
        this.connectorId = new QualifiedNameDTO().from(connectorId);
        this.targetSpecification = targetSpecification;
    }
}
