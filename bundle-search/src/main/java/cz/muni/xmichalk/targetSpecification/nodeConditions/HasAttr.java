package cz.muni.xmichalk.targetSpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.ICondition;
import cz.muni.xmichalk.util.CpmUtils;

import java.util.List;

public class HasAttr implements ICondition<INode> {
    public String attributeNameUri;

    public HasAttr() {
    }

    public HasAttr(String attributeNameUri) {
        this.attributeNameUri = attributeNameUri;
    }

    @Override
    public boolean test(INode node) {
        Object value = CpmUtils.getAttributeValue(node, attributeNameUri);
        return (value != null && !(value instanceof List<?> && ((List<?>) value).isEmpty()));
    }
}
