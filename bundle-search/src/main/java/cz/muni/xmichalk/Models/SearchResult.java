package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.Token;

public record SearchResult(Token token, JsonNode found) {
}
