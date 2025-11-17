package cz.muni.xmichalk.models;

import cz.muni.xmichalk.validity.EValidityCheck;
import org.openprovenance.prov.model.QualifiedName;

import java.util.LinkedHashMap;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public String provServiceUri;
    public boolean pathIntegrity;
    public LinkedHashMap<EValidityCheck, Boolean> pathValidityChecks;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, String provServiceUri, boolean pathIntegrity, LinkedHashMap<EValidityCheck, Boolean> pathValidityChecks) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.provServiceUri = provServiceUri;
        this.pathIntegrity = pathIntegrity;
        this.pathValidityChecks = pathValidityChecks;
    }

}
