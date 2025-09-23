package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class QualifiedNameDTO {
    public String nameSpaceUri;
    public String localPart;

    public QualifiedNameDTO() {
    }

    public QualifiedNameDTO(QualifiedName qualifiedName) {
        this.nameSpaceUri = qualifiedName.getNamespaceURI();
        this.localPart = qualifiedName.getLocalPart();
    }

    public QualifiedNameDTO(String nameSpaceUri, String localName) {
        this.nameSpaceUri = nameSpaceUri;
        this.localPart = localName;
    }
}
