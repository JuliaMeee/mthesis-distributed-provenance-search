package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.InnerNode;

public class InnerNodeDTO implements IDTO<InnerNode> {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO nodeId;

    public InnerNodeDTO() {

    }

    @Override
    public InnerNode toDomainModel() {
        return new InnerNode(
                this.bundleId.toDomainModel(),
                this.nodeId.toDomainModel()
        );
    }

    @Override
    public InnerNodeDTO from(final InnerNode domainModel) {
        this.bundleId = new QualifiedNameDTO().from(domainModel.bundleId);
        this.nodeId = new QualifiedNameDTO().from(domainModel.nodeId);
        return this;
    }
}
