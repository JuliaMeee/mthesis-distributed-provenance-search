package cz.muni.xmichalk.DTO;

import org.openprovenance.apache.commons.lang.builder.ToString;
import org.openprovenance.prov.model.QualifiedName;

public class QualifiedNameDTO {
    public String nameSpaceUri;
    public String localPart;

    public QualifiedNameDTO() {
    }
    
    public QualifiedNameDTO(String nameSpaceUri, String localPart) {
        this.nameSpaceUri = nameSpaceUri;
        this.localPart = localPart;
    }


    public QualifiedNameDTO(QualifiedName domainModel) {
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
    public String toString(){
        return nameSpaceUri + localPart;
    }
}
