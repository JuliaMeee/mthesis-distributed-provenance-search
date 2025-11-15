package cz.muni.xmichalk.dto;

public class PickVersionParamsDTO {
    public QualifiedNameDTO bundleId;
    public String versionPreference;

    public PickVersionParamsDTO() {
    }

    public PickVersionParamsDTO(QualifiedNameDTO bundleId, String versionPreference) {
        this.bundleId = bundleId;
        this.versionPreference = versionPreference;
    }
}