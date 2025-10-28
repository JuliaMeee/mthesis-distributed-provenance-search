package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;

public class SearchParams {
    public boolean searchBackwards;
    public String versionPreference;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParams() {

    }

    public SearchParams(boolean searchBackwards, String versionPreference, String targetType, JsonNode targetSpecification) {
        this.searchBackwards = searchBackwards;
        this.versionPreference = versionPreference;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
