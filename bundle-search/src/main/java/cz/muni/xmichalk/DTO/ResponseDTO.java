package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;

public record ResponseDTO(QualifiedNameDTO bundleId, JsonNode found) { }
