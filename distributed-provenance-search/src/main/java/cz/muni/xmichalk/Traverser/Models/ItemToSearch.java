package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public boolean hasPathIntegrity;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, boolean hasPathIntegrity) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.hasPathIntegrity = hasPathIntegrity;
    }

}
