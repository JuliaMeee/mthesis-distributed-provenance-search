package cz.muni.xmichalk.Models;

import org.openprovenance.prov.model.QualifiedName;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public boolean hasPathIntegrity;
    public boolean isPathValid;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, boolean hasPathIntegrity, boolean isPathValid) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.hasPathIntegrity = hasPathIntegrity;
        this.isPathValid = isPathValid;
    }

}
