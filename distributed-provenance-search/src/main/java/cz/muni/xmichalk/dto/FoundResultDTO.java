package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.models.FoundResult;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.HashMap;

public class FoundResultDTO implements IDTO<FoundResult> {
    public QualifiedNameDTO bundleId;
    public boolean integrity;
    public HashMap<EValidityCheck, Boolean> validityChecks;
    public boolean pathIntegrity;
    public HashMap<EValidityCheck, Boolean> pathValidityChecks;
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
    public FoundResultDTO from(FoundResult domainModel) {
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
