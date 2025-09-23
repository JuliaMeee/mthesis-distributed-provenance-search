package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Traverser.TraverserUtils;
import org.openprovenance.prov.model.QualifiedName;

public class ConnectorDataDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;

    public ConnectorDataDTO() {
    }

    public ConnectorDataDTO(QualifiedName id, QualifiedName referencedBundleId) {
        this.id = new QualifiedNameDTO(id);
        this.referencedBundleId = new QualifiedNameDTO(referencedBundleId);
    }

    public ConnectorDataDTO(INode node) {
        this.id = new QualifiedNameDTO(node.getId());
        this.referencedBundleId = new QualifiedNameDTO(TraverserUtils.getReferencedBundleId(node));
    }
}