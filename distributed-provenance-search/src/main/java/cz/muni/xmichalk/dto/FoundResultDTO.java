package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.models.ResultFromBundle;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.HashMap;

public class FoundResultDTO {
    public QualifiedNameDTO bundleId;
    public boolean integrity;
    public HashMap<EValidityCheck, Boolean> validityChecks;
    public boolean pathIntegrity;
    public HashMap<EValidityCheck, Boolean> pathValidityChecks;
    public JsonNode result;


    public FoundResultDTO() {

    }

    public FoundResultDTO(QualifiedNameDTO bundleId, boolean integrity, HashMap<EValidityCheck, Boolean> validityChecks, boolean pathIntegrity, HashMap<EValidityCheck, Boolean> pathValidityChecks, JsonNode result) {
        this.bundleId = bundleId;
        this.integrity = integrity;
        this.validityChecks = validityChecks;
        this.pathIntegrity = pathIntegrity;
        this.pathValidityChecks = pathValidityChecks;
        this.result = result;
    }

    public FoundResultDTO from(ResultFromBundle domainModel) {
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
