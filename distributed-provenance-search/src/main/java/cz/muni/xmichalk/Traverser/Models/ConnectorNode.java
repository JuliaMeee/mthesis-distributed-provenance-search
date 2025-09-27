package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Traverser.TraverserUtils;
import org.openprovenance.prov.model.QualifiedName;

public class ConnectorNode {
    public QualifiedName id;
    public QualifiedName referencedBundleId;

    public ConnectorNode() {
    }

    public ConnectorNode(QualifiedName id, QualifiedName referencedBundleId) {
        this.id = id;
        this.referencedBundleId = referencedBundleId;
    }

    public ConnectorNode(INode node) {
        this(node.getId(), TraverserUtils.getReferencedBundleId(node));
    }
}
