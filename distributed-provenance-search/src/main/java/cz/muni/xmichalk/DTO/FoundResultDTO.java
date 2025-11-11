package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.Models.FoundResult;

import java.util.HashMap;

public class FoundResultDTO implements IDTO<FoundResult> {
    public QualifiedNameDTO bundleId;
    public boolean integrity;
    public HashMap<EValiditySpecification, Boolean> validityChecks;
    public boolean pathIntegrity;
    public HashMap<EValiditySpecification, Boolean> pathValidityChecks;
    public JsonNode result;


    public FoundResultDTO() {

    }

    @Override
    public FoundResult toDomainModel() {
        return new FoundResult(
                this.bundleId.toDomainModel(),
                this.result,
                this.integrity,
                this.validityChecks,
                this.pathIntegrity,
                this.pathValidityChecks
        );
    }

    @Override
    public FoundResultDTO from(final FoundResult domainModel) {
        if (domainModel == null) {
            return null;
        }
        this.bundleId = new QualifiedNameDTO().from(domainModel.bundleId);
        this.result = domainModel.result;
        this.integrity = domainModel.integrity;
        this.validityChecks = new HashMap<>(domainModel.validityChecks);
        this.pathIntegrity = domainModel.pathIntegrity;
        this.pathValidityChecks = new HashMap<>(domainModel.pathValidityChecks);
        return this;
    }
}
