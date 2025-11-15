package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.dto.token.Token;

public class BundleSearchResultDTO {
    public Token token;
    public JsonNode found;

    public BundleSearchResultDTO() {
    }

    public BundleSearchResultDTO(Token token, JsonNode found) {
        this.token = token;
        this.found = found;
    }
}