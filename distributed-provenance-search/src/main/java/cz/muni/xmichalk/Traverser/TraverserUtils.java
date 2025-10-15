package cz.muni.xmichalk.Traverser;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;

public class TraverserUtils {
    private static final String cpmNamespace = "https://www.commonprovenancemodel.org/";

    
    private static boolean isReferencedBundleIdAttribute(QualifiedName attrName) {
        return (attrName.getUri().startsWith(cpmNamespace) && attrName.getLocalPart().equals("referencedBundleId"));
    }

    public static QualifiedName getReferencedBundleId(INode connectorNode) {
        for (Element element : connectorNode.getElements()) {
            for (Other other : element.getOther()) {
                if (isReferencedBundleIdAttribute(other.getElementName())) {
                    return other.getValue() instanceof QualifiedName ? (QualifiedName) other.getValue() : null;
                }
            }
        }

        return null;
    }
}
