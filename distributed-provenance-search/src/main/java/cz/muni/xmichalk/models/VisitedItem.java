package cz.muni.xmichalk.models;

import org.openprovenance.prov.model.QualifiedName;

public class VisitedItem {
    public QualifiedName bundleId;

    public VisitedItem(QualifiedName bundleId) {
        this.bundleId = bundleId;
    }
}
