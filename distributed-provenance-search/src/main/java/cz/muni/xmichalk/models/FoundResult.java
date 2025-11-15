package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.validity.EValidityCheck;
import org.openprovenance.prov.model.QualifiedName;

import java.util.Map;

public class FoundResult {
    public QualifiedName bundleId;
    public JsonNode result;
    public boolean integrity;
    public Map<EValidityCheck, Boolean> validityChecks;
    public boolean pathIntegrity;
    public Map<EValidityCheck, Boolean> pathValidityChecks;


    public FoundResult() {

    }

    public FoundResult(QualifiedName bundleId,
                       JsonNode result,
                       boolean integrity,
                       Map<EValidityCheck, Boolean> validityChecks,
                       boolean pathIntegrity,
                       Map<EValidityCheck, Boolean> pathValidityChecks) {
        this.bundleId = bundleId;
        this.result = result;
        this.integrity = integrity;
        this.validityChecks = validityChecks;
        this.pathIntegrity = pathIntegrity;
        this.pathValidityChecks = pathValidityChecks;
    }
}
