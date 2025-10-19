package cz.muni.xmichalk.BundleSearch.General;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.vanilla.QualifiedName;

import static cz.muni.xmichalk.Util.Constants.PROV_URI;

public class NodeAttributeSearcher {
    public final QualifiedName startTimeAttributeName = new QualifiedName(PROV_URI, "startTime", "prov");
    public final QualifiedName endTimeAttributeName = new QualifiedName(PROV_URI, "startTime", "prov");
    public final QualifiedName locationAttributeName = new QualifiedName(PROV_URI, "location", "prov");
    public final QualifiedName provTypeAttributeName = new QualifiedName(PROV_URI, "type", "prov");
    public final QualifiedName provLabelAttributeName = new QualifiedName(PROV_URI, "label", "prov");


    public Object tryGetValue(INode node, org.openprovenance.prov.model.QualifiedName attributeName) {
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
        if (attributeName.equals(provTypeAttributeName)){
            for (Element element : node.getElements()) {
                for (Type type : element.getType()) {
                    if (type.getElementName().equals(attributeName)) {
                        return type.getValue();
                    }
                }
            }
            
        }
        if (attributeName.equals(provLabelAttributeName)) {
            for (Element element : node.getElements()) {
                if (element.getLabel() != null){
                    return element.getLabel();
                }
            }
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
