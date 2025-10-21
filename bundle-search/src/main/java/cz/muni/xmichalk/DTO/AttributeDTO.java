package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.databind.JsonNode;

public record AttributeDTO(QualifiedNameDTO name, JsonNode value){}
