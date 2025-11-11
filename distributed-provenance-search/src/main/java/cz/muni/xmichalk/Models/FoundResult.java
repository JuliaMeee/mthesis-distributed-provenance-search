package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import org.openprovenance.prov.model.QualifiedName;

import java.util.Map;

public class FoundResult {
    public QualifiedName bundleId;
    public JsonNode result;
    public boolean integrity;
    public Map<EValiditySpecification, Boolean> validityChecks;
    public boolean pathIntegrity;
    public Map<EValiditySpecification, Boolean> pathValidityChecks;


    public FoundResult() {

    }

    public FoundResult(QualifiedName bundleId,
                       JsonNode result,
                       boolean integrity,
                       Map<EValiditySpecification, Boolean> validityChecks,
                       boolean pathIntegrity,
                       Map<EValiditySpecification, Boolean> pathValidityChecks) {
        this.bundleId = bundleId;
        this.result = result;
        this.integrity = integrity;
        this.validityChecks = validityChecks;
        this.pathIntegrity = pathIntegrity;
        this.pathValidityChecks = pathValidityChecks;
    }
}
