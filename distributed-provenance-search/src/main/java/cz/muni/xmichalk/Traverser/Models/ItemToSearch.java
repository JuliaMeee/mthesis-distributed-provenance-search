package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class ItemToSearch {
    public QualifiedName bundleId;
    public QualifiedName connectorId;
    public ECredibility pathCredibility;

    public ItemToSearch(QualifiedName bundleId, QualifiedName connectorId, ECredibility pathCredibility) {
        this.bundleId = bundleId;
        this.connectorId = connectorId;
        this.pathCredibility = pathCredibility;
    }

}
