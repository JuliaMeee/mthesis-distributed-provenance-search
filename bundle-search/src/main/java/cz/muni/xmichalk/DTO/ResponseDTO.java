package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.Token;

public record ResponseDTO(QualifiedNameDTO bundleId, Token token, JsonNode found) { }
