package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.searchPriority.ESearchPriority;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.List;

public class SearchParams {
    public boolean searchBackwards;
    public String versionPreference;
    public ESearchPriority searchPriority;
    public List<EValidityCheck> validityChecks;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParams() {

    }

    public SearchParams(boolean searchBackwards, String versionPreference, ESearchPriority searchPriority, List<EValidityCheck> validityChecks, String targetType, JsonNode targetSpecification) {
        this.searchBackwards = searchBackwards;
        this.versionPreference = versionPreference;
        this.searchPriority = searchPriority;
        this.validityChecks = validityChecks;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
