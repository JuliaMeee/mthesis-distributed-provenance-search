package cz.muni.xmichalk.Traverser.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.Traverser.Models.FoundResult;

public class FoundResultDTO implements IDTO<FoundResult> {
    public QualifiedNameDTO bundleId;
    public boolean pathIntegrity;
    public boolean integrity;
    public boolean pathValidity;
    public boolean validity;
    public JsonNode result;


    public FoundResultDTO() {

    }

    @Override
    public FoundResult toDomainModel() {
        return new FoundResult(
                this.bundleId.toDomainModel(),
                this.result,
                this.pathIntegrity,
                this.integrity,
                this.pathValidity,
                this.validity
        );
    }

    @Override
    public FoundResultDTO from(final FoundResult domainModel) {
        this.bundleId = new QualifiedNameDTO().from(domainModel.bundleId);
        this.result = domainModel.result;
        this.pathIntegrity = domainModel.pathIntegrity;
        this.integrity = domainModel.integrity;
        this.pathValidity = domainModel.pathValidity;
        this.validity = domainModel.validity;
        return this;
    }
}
