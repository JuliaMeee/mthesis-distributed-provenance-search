package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class VisitedEntry {
    public QualifiedName bundleId;
    public ECredibility pathCredibility;
    public ECredibility bundleCredibility;

    public VisitedEntry(QualifiedName bundleId, ECredibility pathCredibility, ECredibility bundleCredibility) {
        this.bundleId = bundleId;
        this.pathCredibility = pathCredibility;
        this.bundleCredibility = bundleCredibility;
    }
}
