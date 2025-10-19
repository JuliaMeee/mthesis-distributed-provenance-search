package cz.muni.xmichalk.Traverser.DTO;

public class ConnectorDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;
    public QualifiedNameDTO referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String hashAlg;

    public ConnectorDTO() {
    }
}
