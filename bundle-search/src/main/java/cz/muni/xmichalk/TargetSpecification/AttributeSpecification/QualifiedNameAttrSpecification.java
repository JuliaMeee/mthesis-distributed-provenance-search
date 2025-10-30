package cz.muni.xmichalk.TargetSpecification.AttributeSpecification;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Util.CpmUtils;
import org.openprovenance.prov.model.QualifiedName;

public class QualifiedNameAttrSpecification extends AttrSpecification {
    public String uriRegex;

    public QualifiedNameAttrSpecification() {
    }

    public QualifiedNameAttrSpecification(String attributeNameUri, String regex) {
        this.attributeNameUri = attributeNameUri;
        this.uriRegex = regex;
    }

    public boolean test(INode node) {
        if (uriRegex == null) return true;

        try {
            return CpmUtils.hasAttributeTargetValue(node, attributeNameUri, QualifiedName.class, (qn) ->
                    qn.getUri().matches(uriRegex));
        } catch (Exception e) {
            return false;
        }
    }
}
