package cz.muni.xmichalk.BundleSearcher;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.vanilla.QualifiedName;

public class NodeSearcher {
    public final String provNamespacePrefix = "prov";
    public final String provNamespaceUrl = "http://www.w3.org/ns/prov#";
    public final String cpmNamespacePrefix = "cpm";
    public final String cpmNamespaceUrl = "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/";
    public final QualifiedName startTimeAttributeName = new QualifiedName(provNamespaceUrl, "startTime", provNamespacePrefix);
    public final QualifiedName endTimeAttributeName = new QualifiedName(provNamespaceUrl, "startTime", provNamespacePrefix);
    public final QualifiedName locationAttributeName = new QualifiedName(provNamespaceUrl, "location", provNamespacePrefix);

    public Object tryGetValue(INode node, QualifiedName attributeName) {
        if (node.getKind() == StatementOrBundle.Kind.PROV_ACTIVITY) {
            if (attributeName.equals(startTimeAttributeName)) {
                return ((Activity) node.getAnyElement()).getStartTime();
            } else if (attributeName.equals(endTimeAttributeName)) {
                return ((Activity) node.getAnyElement()).getEndTime();
            }
        }
        if (attributeName.equals(locationAttributeName)) {
            return node.getAnyElement().getLocation();
        }
        if (attributeName.getLocalPart().equals("id")) {
            return node.getId();
        }
        for (Element element : node.getElements()) {
            for (Other other : element.getOther()) {
                org.openprovenance.prov.model.QualifiedName name = other.getElementName();
                if (name.equals(attributeName)) {
                    return other.getValue();
                }
            }
        }

        return null;
    }
}
