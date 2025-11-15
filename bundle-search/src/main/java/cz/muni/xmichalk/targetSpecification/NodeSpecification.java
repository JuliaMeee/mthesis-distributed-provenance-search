package cz.muni.xmichalk.targetSpecification;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.attributeSpecification.AttrSpecification;
import cz.muni.xmichalk.util.CpmUtils;
import org.openprovenance.prov.model.StatementOrBundle;

import java.util.List;

public class NodeSpecification implements ITestableSpecification<INode> {
    public String idUriRegex;
    public StatementOrBundle.Kind isKind;
    public StatementOrBundle.Kind isNotKind;
    public List<String> hasAttributes;
    public List<AttrSpecification> hasAttributeValues;

    public NodeSpecification() {
    }

    public NodeSpecification(String idUriRegex, StatementOrBundle.Kind isKind, StatementOrBundle.Kind isNotKind, List<String> hasAttributes, List<AttrSpecification> hasAttributeValues) {
        this.idUriRegex = idUriRegex;
        this.isKind = isKind;
        this.isNotKind = isNotKind;
        this.hasAttributes = hasAttributes;
        this.hasAttributeValues = hasAttributeValues;
    }

    public boolean test(INode node) {
        if (idUriRegex != null) {
            if (!node.getId().getUri().matches(idUriRegex)) {
                return false;
            }
        }

        if (isKind != null) {
            if (!node.getKind().equals(isKind)) {
                return false;
            }
        }

        if (isNotKind != null) {
            if (node.getKind().equals(isNotKind)) {
                return false;
            }
        }

        if (hasAttributes != null) {
            for (String attr : hasAttributes) {
                var value = CpmUtils.getAttributeValue(node, attr);
                if (value == null || (value instanceof List<?> && ((List<?>) value).isEmpty())) {
                    return false;
                }
            }
        }

        if (hasAttributeValues != null) {
            for (AttrSpecification attrRequirement : hasAttributeValues) {
                if (!attrRequirement.test(node)) {
                    return false;
                }
            }
        }

        return true;
    }
}
