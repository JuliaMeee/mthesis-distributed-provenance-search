package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

public class SearchResultEntry {
    public QualifiedName bundleId;
    public INode node;

    public SearchResultEntry(QualifiedName bundleId, INode node) {
        this.bundleId = bundleId;
        this.node = node;
    }
}
