package cz.muni.xmichalk.Traverser.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentLoader.TokenDTO.Token;

public class BundleSearchResponseDTO {
    public QualifiedNameDTO bundleId;
    public Token token;
    public JsonNode found;

    public BundleSearchResponseDTO() {

    }

    public BundleSearchResponseDTO(QualifiedNameDTO bundleId, Token token, JsonNode found) {
        this.bundleId = bundleId;
        this.found = found;
    }
}
