package cz.muni.xmichalk.DTO;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Util.CpmUtils;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import static cz.muni.xmichalk.Util.AttributeNames.*;

public class ConnectorDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;
    public QualifiedNameDTO referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String hashAlg;

    public ConnectorDTO() {
    }
    
    public ConnectorDTO(INode node) {
        id = new QualifiedNameDTO(node.getId());
        
        var referencedBundleIdValue = CpmUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_ID);
        referencedBundleId = referencedBundleIdValue == null ? null : 
                new QualifiedNameDTO((QualifiedName) referencedBundleIdValue);
        
        var referencedMetaBundleIdValue = CpmUtils.getAttributeValue(node, ATTR_REFERENCED_META_BUNDLE_ID);
        referencedMetaBundleId = referencedMetaBundleIdValue == null ? null :
                new QualifiedNameDTO((QualifiedName) referencedMetaBundleIdValue);


        LangString bundleHashValueObj = (LangString) CpmUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_HASH_VALUE);
        referencedBundleHashValue = bundleHashValueObj != null ? bundleHashValueObj.getValue() : null;

        LangString hashAlgObj = (LangString) CpmUtils.getAttributeValue(node, ATTR_HASH_ALG);
        hashAlg = hashAlgObj != null ? hashAlgObj.getValue() : null;
    }
}
