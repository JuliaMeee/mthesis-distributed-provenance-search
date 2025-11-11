package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.SearchPriority.ESearchPriority;

import java.util.List;

public class SearchParams {
    public boolean searchBackwards;
    public String versionPreference;
    public ESearchPriority searchPriority;
    public List<EValiditySpecification> validityChecks;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParams() {

    }

    public SearchParams(boolean searchBackwards, String versionPreference, ESearchPriority searchPriority, List<EValiditySpecification> validityChecks, String targetType, JsonNode targetSpecification) {
        this.searchBackwards = searchBackwards;
        this.versionPreference = versionPreference;
        this.searchPriority = searchPriority;
        this.validityChecks = validityChecks;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
