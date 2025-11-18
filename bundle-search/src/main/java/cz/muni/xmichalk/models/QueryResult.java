package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.documentLoader.storageDTO.Token;

public class QueryResult {
    public Token token;
    public JsonNode result;

    public QueryResult() {
    }

    public QueryResult(Token token, JsonNode result) {
        this.token = token;
        this.result = result;
    }
}