package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.Models.FoundResult;

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
        this.pathIntegrity = domainModel.hasPathIntegrity;
        this.integrity = domainModel.hasIntegrity;
        this.pathValidity = domainModel.isPathValid;
        this.validity = domainModel.isValid;
        return this;
    }
}
