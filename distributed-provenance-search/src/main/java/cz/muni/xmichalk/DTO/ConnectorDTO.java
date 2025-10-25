package cz.muni.xmichalk.DTO;

public class ConnectorDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;
    public QualifiedNameDTO referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String hashAlg;

    public ConnectorDTO() {
    }
}
