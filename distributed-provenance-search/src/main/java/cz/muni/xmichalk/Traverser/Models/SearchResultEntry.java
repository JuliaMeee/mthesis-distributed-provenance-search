package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class SearchResultEntry {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO nodeId;

    public SearchResultEntry() {

    }

    public SearchResultEntry(QualifiedName bundleId, QualifiedName nodeId) {
        this.bundleId = new QualifiedNameDTO(bundleId);
        this.nodeId = new QualifiedNameDTO(nodeId);
    }
}
