package cz.muni.xmichalk.Traverser.DTO;

public class BundleSearchResponseDTO {
    public String connectors;
    public Object found;

    public BundleSearchResponseDTO(String connectors, Object found) {
        this.connectors = connectors;
        this.found = found;
    }
}
