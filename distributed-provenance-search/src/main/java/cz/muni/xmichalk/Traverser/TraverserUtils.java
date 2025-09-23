package cz.muni.xmichalk.Traverser;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Traverser.Models.SearchParamsDTO;
import cz.muni.xmichalk.Traverser.Models.TargetSpecification;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;

import java.util.Objects;
import java.util.function.Predicate;

public class TraverserUtils {
    private static final String cpmNamespace = "https://www.commonprovenancemodel.org/";

    public static boolean isMissingRequiredParams(SearchParamsDTO params) {
        return params.bundlePrefixUrl == null || params.bundleLocalName == null ||
                params.connectorPrefixUrl == null || params.connectorLocalName == null ||
                params.targetSpecification == null;
    }

    public static Predicate<INode> translateToPredicate(TargetSpecification targetSpecification) {
        return node -> Objects.equals(node.getId().getLocalPart(), targetSpecification.localName);
    }

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
