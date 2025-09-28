package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class VisitedItem {
    public QualifiedName bundleId;
    public ECredibility pathCredibility;
    public ECredibility bundleCredibility;

    public VisitedItem(QualifiedName bundleId, ECredibility pathCredibility, ECredibility bundleCredibility) {
        this.bundleId = bundleId;
        this.pathCredibility = pathCredibility;
        this.bundleCredibility = bundleCredibility;
    }
}
