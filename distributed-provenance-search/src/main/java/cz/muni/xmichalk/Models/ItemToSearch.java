package cz.muni.xmichalk.Models;

import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import org.openprovenance.prov.model.QualifiedName;

import java.util.Map;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public String provServiceUri;
    public boolean pathIntegrity;
    public Map<EValiditySpecification, Boolean> pathValidityChecks;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, String provServiceUri, boolean pathIntegrity, Map<EValiditySpecification, Boolean> pathValidityChecks) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.provServiceUri = provServiceUri;
        this.pathIntegrity = pathIntegrity;
        this.pathValidityChecks = pathValidityChecks;
    }

}
