package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.dto.token.Token;

public class BundleQueryResultDTO {
    public Token token;
    public JsonNode result;

    public BundleQueryResultDTO() {
    }

    public BundleQueryResultDTO(Token token, JsonNode result) {
        this.token = token;
        this.result = result;
    }
}