package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public boolean pathHasIntegrity = false;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
    }

}
