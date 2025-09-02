package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class VisitedEntry {
    public QualifiedName bundleId;
    public ECredibility credibility;

    public VisitedEntry(QualifiedName bundleId, ECredibility credibility) {
        this.bundleId = bundleId;
        this.credibility = credibility;
    }
}
