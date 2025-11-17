package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.documentLoader.storageDTO.Token;

public class SearchResult {
    public Token token;
    public JsonNode found;

    public SearchResult() {
    }

    public SearchResult(Token token, JsonNode found) {
        this.token = token;
        this.found = found;
    }
}