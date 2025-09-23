package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class SearchParamsDTO {
    public String bundlePrefixUrl;
    public String bundleLocalName;
    public String connectorPrefixUrl;
    public String connectorLocalName;
    public TargetSpecification targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(String bundlePrefixUrl, String bundleLocalName, String connectorPrefixUrl, String connectorLocalName, TargetSpecification targetSpecification) {
        this.bundlePrefixUrl = bundlePrefixUrl;
        this.bundleLocalName = bundleLocalName;
        this.connectorPrefixUrl = connectorPrefixUrl;
        this.connectorLocalName = connectorLocalName;
        this.targetSpecification = targetSpecification;
    }

    public SearchParamsDTO(QualifiedName bundleId, QualifiedName connectorId, TargetSpecification targetSpecification) {
        this.bundlePrefixUrl = bundleId.getNamespaceURI();
        this.bundleLocalName = bundleId.getLocalPart();
        this.connectorPrefixUrl = connectorId.getNamespaceURI();
        this.connectorLocalName = connectorId.getLocalPart();
        this.targetSpecification = targetSpecification;
    }
}
