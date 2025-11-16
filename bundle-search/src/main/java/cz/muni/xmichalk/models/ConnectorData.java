package cz.muni.xmichalk.models;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.util.CpmUtils;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import static cz.muni.xmichalk.util.AttributeNames.*;

public class ConnectorData {
    public QualifiedNameData id;
    public QualifiedNameData referencedConnectorId;
    public QualifiedNameData referencedBundleId;
    public QualifiedNameData referencedMetaBundleId;
    public String referencedBundleHashValue;
    public String provenanceServiceUri;
    public String hashAlg;

    public ConnectorData() {
    }

    public ConnectorData(INode node) {
        id = new QualifiedNameData().from(node.getId());

        QualifiedName referencedConnectorIdValue = CpmUtils.getConnectorIdInReferencedBundle(node);
        referencedConnectorId =
                new QualifiedNameData().from(referencedConnectorIdValue == null ? node.getId() : referencedConnectorIdValue);

        Object referencedBundleIdValue = CpmUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_ID);
        referencedBundleId = referencedBundleIdValue == null ? null :
                new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedBundleIdValue);

        Object referencedMetaBundleIdValue = CpmUtils.getAttributeValue(node, ATTR_REFERENCED_META_BUNDLE_ID);
        referencedMetaBundleId = referencedMetaBundleIdValue == null ? null :
                new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedMetaBundleIdValue);

        Object provServiceUriValue = CpmUtils.getAttributeValue(node, ATTR_PROVENANCE_SERVICE_URI);
        provenanceServiceUri = provServiceUriValue == null ? null : ((LangString) provServiceUriValue).getValue();


        LangString bundleHashValueObj = (LangString) CpmUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_HASH_VALUE);
        referencedBundleHashValue = bundleHashValueObj != null ? bundleHashValueObj.getValue() : null;

        LangString hashAlgObj = (LangString) CpmUtils.getAttributeValue(node, ATTR_HASH_ALG);
        hashAlg = hashAlgObj != null ? hashAlgObj.getValue() : null;
    }
}
