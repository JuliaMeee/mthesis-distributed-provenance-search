package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class FoundResult {
    public QualifiedName bundleId;
    public QualifiedName nodeId;
    public ECredibility credibility;

    public FoundResult() {

    }

    public FoundResult(QualifiedName bundleId, QualifiedName nodeId, ECredibility credibility) {
        this.bundleId = bundleId;
        this.nodeId = nodeId;
        this.credibility = credibility;
    }
}
