package cz.muni.xmichalk.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.documentLoader.storageDTO.Token;

public record SearchResult(Token token, JsonNode found) {
}
