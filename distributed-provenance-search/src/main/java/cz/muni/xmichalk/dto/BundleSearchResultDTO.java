package cz.muni.xmichalk.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.dto.token.Token;

public record BundleSearchResultDTO(Token token, JsonNode found) {
}
