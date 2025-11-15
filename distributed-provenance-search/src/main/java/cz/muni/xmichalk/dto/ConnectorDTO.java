package cz.muni.xmichalk.dto;

public class ConnectorDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedConnectorId;
    public QualifiedNameDTO referencedBundleId;
    public QualifiedNameDTO referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String hashAlg;
    public String provenanceServiceUri;

    public ConnectorDTO() {
    }
}
