package cz.muni.xmichalk.querySpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.AttributeUtils;
import org.openprovenance.prov.model.QualifiedName;

public class HasAttrQualifiedNameValue implements ICondition<INode> {
    public String attributeNameUri;
    public String uriRegex;

    public HasAttrQualifiedNameValue() {
    }

    public HasAttrQualifiedNameValue(String attributeNameUri, String regex) {
        this.attributeNameUri = attributeNameUri;
        this.uriRegex = regex;
    }

    @Override
    public boolean test(INode node) {
        if (attributeNameUri == null || uriRegex == null) return true;

        try {
            return AttributeUtils.hasAttributeTargetValue(node, attributeNameUri, QualifiedName.class, (qn) ->
                    qn.getUri().matches(uriRegex));
        } catch (Exception e) {
            return false;
        }
    }
}
