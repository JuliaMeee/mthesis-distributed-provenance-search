package cz.muni.xmichalk.Traverser.DTO;

import com.fasterxml.jackson.databind.JsonNode;

public class BundleSearchResponseDTO {
    public QualifiedNameDTO bundleId;
    public JsonNode found;

    public BundleSearchResponseDTO(QualifiedNameDTO bundleId, JsonNode found) {
        this.bundleId = bundleId;
        this.found = found;
    }
}
