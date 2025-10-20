package cz.muni.xmichalk.DTO;


import cz.muni.xmichalk.BundleSearch.ETargetType;
import io.swagger.v3.oas.annotations.media.Schema;

public record TargetTypeInfoDTO (
        @Schema(
                description = "the target type",
                example = "CONNECTORS"
        )
        ETargetType targetType,
        @Schema(
                description = "info about the target type",
                example = "Finds connectors matching the given specification, returns their relevant information in ConnectorDTO object. Expects target specification to be either 'backward' or 'forward'."
        )
        String description) {
}