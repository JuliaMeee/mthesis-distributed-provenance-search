package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.ECredibility;
import cz.muni.xmichalk.Traverser.Models.FoundResult;

public class FoundResultDTO implements IDTO<FoundResult> {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO nodeId;
    public ECredibility credibility;

    public FoundResultDTO() {

    }

    @Override
    public FoundResult toDomainModel() {
        return new FoundResult(
                this.bundleId.toDomainModel(),
                this.nodeId.toDomainModel(),
                this.credibility
        );
    }

    @Override
    public FoundResultDTO from(final FoundResult domainModel) {
        this.bundleId = new QualifiedNameDTO().from(domainModel.bundleId);
        this.nodeId = new QualifiedNameDTO().from(domainModel.nodeId);
        this.credibility = domainModel.credibility;
        return this;
    }
}
