package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class InnerNode {
    public QualifiedName bundleId;
    public QualifiedName nodeId;

    public InnerNode() {

    }

    public InnerNode(QualifiedName bundleId, QualifiedName nodeId) {
        this.bundleId = bundleId;
        this.nodeId = nodeId;
    }
}
