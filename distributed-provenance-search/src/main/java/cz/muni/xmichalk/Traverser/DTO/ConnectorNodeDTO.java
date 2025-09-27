package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.ConnectorNode;

public class ConnectorNodeDTO implements IDTO<ConnectorNode> {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;

    public ConnectorNodeDTO() {
    }

    @Override
    public ConnectorNode toDomainModel() {
        return new ConnectorNode(
                id.toDomainModel(),
                referencedBundleId.toDomainModel()
        );
    }

    @Override
    public ConnectorNodeDTO from(final ConnectorNode domainModel) {
        this.id = new QualifiedNameDTO().from(domainModel.id);
        this.referencedBundleId = new QualifiedNameDTO().from(domainModel.referencedBundleId);
        return this;
    }
}