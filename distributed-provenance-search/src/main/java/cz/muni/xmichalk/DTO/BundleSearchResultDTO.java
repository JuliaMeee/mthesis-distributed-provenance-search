package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DTO.Token.Token;

public record BundleSearchResultDTO(Token token, JsonNode found) {
}
