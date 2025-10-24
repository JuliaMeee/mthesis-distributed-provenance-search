package cz.muni.xmichalk.Traverser.Models;

import com.fasterxml.jackson.databind.JsonNode;

public class SearchParams {
    public boolean searchBackwards;
    public String targetType;
    public JsonNode targetSpecification;

    public SearchParams() {

    }

    public SearchParams(boolean searchBackwards, String targetType, JsonNode targetSpecification) {
        this.searchBackwards = searchBackwards;
        this.targetType = targetType;
        this.targetSpecification = targetSpecification;
    }
}
