package cz.muni.xmichalk.DTO;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class ResponseDTO {
    public QualifiedNameDTO bundleId;
    
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class"
            // include class info for polymorphic deserialization
    )
    public Object found;
    
    public ResponseDTO(QualifiedNameDTO bundleId, Object found) {
        this.bundleId = bundleId;
        this.found = found;
    }
}
