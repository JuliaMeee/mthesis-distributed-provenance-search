package cz.muni.xmichalk.Models;

import org.openprovenance.prov.model.QualifiedName;

public class VisitedItem {
    public QualifiedName bundleId;

    public VisitedItem(QualifiedName bundleId) {
        this.bundleId = bundleId;
    }
}
