package cz.muni.xmichalk.Models;

import org.openprovenance.prov.model.QualifiedName;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public String provServiceUri;
    public boolean hasPathIntegrity;
    public boolean isPathValid;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, String provServiceUri, boolean hasPathIntegrity, boolean isPathValid) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.provServiceUri = provServiceUri;
        this.hasPathIntegrity = hasPathIntegrity;
        this.isPathValid = isPathValid;
    }

}
