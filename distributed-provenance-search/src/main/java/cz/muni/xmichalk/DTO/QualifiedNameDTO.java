package cz.muni.xmichalk.DTO;

import org.openprovenance.prov.model.QualifiedName;

public class QualifiedNameDTO implements IDTO<QualifiedName> {
    public String nameSpaceUri;
    public String localPart;

    public QualifiedNameDTO() {
    }

    @Override
    public QualifiedName toDomainModel() {
        return new org.openprovenance.prov.vanilla.QualifiedName(
                this.nameSpaceUri,
                this.localPart,
                null
        );
    }

    @Override
    public QualifiedNameDTO from(final QualifiedName domainModel) {
        this.nameSpaceUri = domainModel.getNamespaceURI();
        this.localPart = domainModel.getLocalPart();
        return this;
    }
}
