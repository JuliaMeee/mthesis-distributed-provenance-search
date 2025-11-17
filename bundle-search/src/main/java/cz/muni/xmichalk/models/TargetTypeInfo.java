package cz.muni.xmichalk.models;

import cz.muni.xmichalk.bundleSearch.ETargetType;
import io.swagger.v3.oas.annotations.media.Schema;

public class TargetTypeInfo {
    @Schema(
            description = "the target type",
            example = "CONNECTORS"
    )
    public ETargetType targetType;

    @Schema(
            description = "info about the target type",
            example = "Finds connectors matching the given specification, returns their relevant information in ConnectorDTO object. Expects target specification to be either 'backward' or 'forward'."
    )
    public String description;

    public TargetTypeInfo() {
    }

    public TargetTypeInfo(ETargetType targetType, String description) {
        this.targetType = targetType;
        this.description = description;
    }
}