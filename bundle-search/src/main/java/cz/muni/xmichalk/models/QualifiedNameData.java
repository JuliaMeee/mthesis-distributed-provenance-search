package cz.muni.xmichalk.models;

import org.openprovenance.prov.model.QualifiedName;

public class QualifiedNameData {
    public String nameSpaceUri;
    public String localPart;

    public QualifiedNameData() {
    }

    public QualifiedNameData(String nameSpaceUri, String localPart) {
        this.nameSpaceUri = nameSpaceUri;
        this.localPart = localPart;
    }


    public QualifiedNameData(QualifiedName domainModel) {
        this.nameSpaceUri = domainModel.getNamespaceURI();
        this.localPart = domainModel.getLocalPart();
    }

    public QualifiedName toQN() {
        return new org.openprovenance.prov.vanilla.QualifiedName(
                this.nameSpaceUri,
                this.localPart,
                null
        );
    }

    @Override
    public String toString() {
        return nameSpaceUri + localPart;
    }
}
