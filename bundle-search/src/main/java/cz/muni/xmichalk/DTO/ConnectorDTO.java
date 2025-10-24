package cz.muni.xmichalk.DTO;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.General.NodeAttributeSearcher;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import static cz.muni.xmichalk.Util.Constants.*;

public class ConnectorDTO {
    public QualifiedNameDTO id;
    public QualifiedNameDTO referencedBundleId;
    public QualifiedNameDTO referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String hashAlg;

    public ConnectorDTO() {
    }
    
    public ConnectorDTO(INode node) {
        var nodeSearcher = new NodeAttributeSearcher();
        
        id = new QualifiedNameDTO(node.getId());
        
        var referencedBundleIdValue = nodeSearcher.tryGetValue(node, ATTR_REFERENCED_BUNDLE_ID);
        referencedBundleId = referencedBundleIdValue == null ? null : 
                new QualifiedNameDTO((QualifiedName) referencedBundleIdValue);
        
        var referencedMetaBundleIdValue = nodeSearcher.tryGetValue(node, ATTR_REFERENCED_META_BUNDLE_ID);
        referencedMetaBundleId = referencedMetaBundleIdValue == null ? null :
                new QualifiedNameDTO((QualifiedName) referencedMetaBundleIdValue);


        LangString bundleHashValueObj = (LangString) nodeSearcher.tryGetValue(node, ATTR_REFERENCED_BUNDLE_HASH_VALUE);
        referencedBundleHashValue = bundleHashValueObj != null ? bundleHashValueObj.getValue() : null;

        LangString hashAlgObj = (LangString) nodeSearcher.tryGetValue(node, ATTR_HASH_ALG);
        hashAlg = hashAlgObj != null ? hashAlgObj.getValue() : null;
    }
}
