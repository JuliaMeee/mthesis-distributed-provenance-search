package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.searchPriority.ESearchPriority;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.List;

public class SearchParamsDTO {
    public QualifiedNameDTO bundleId;
    public QualifiedNameDTO startNodeId;
    public String versionPreference;
    public ESearchPriority searchPriority;
    public List<EValidityCheck> validityChecks;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParamsDTO() {

    }

    public SearchParamsDTO(QualifiedNameDTO bundleId, QualifiedNameDTO connectorId, String versionPreference, ESearchPriority searchPriority, List<EValidityCheck> validityChecks, String targetType, JsonNode targetSpecification) {
        this.bundleId = bundleId;
        this.startNodeId = connectorId;
        this.versionPreference = versionPreference;
        this.searchPriority = searchPriority;
        this.validityChecks = validityChecks;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
