package cz.muni.xmichalk.querySpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.AttributeUtils;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class HasAttrTimestampValue implements ICondition<INode> {
    public String attributeNameUri;
    public String isEqual;
    public String isBefore;
    public String isAfter;

    public HasAttrTimestampValue() {
    }

    public HasAttrTimestampValue(String attributeNameUri, String isEqual, String isBefore, String isAfter) {
        this.attributeNameUri = attributeNameUri;
        this.isEqual = isEqual;
        this.isBefore = isBefore;
        this.isAfter = isAfter;
    }

    @Override
    public boolean test(INode node) {
        try {
            XMLGregorianCalendar value = (XMLGregorianCalendar) AttributeUtils.getAttributeValue(node, attributeNameUri);
            if (value == null) {
                return false;
            }
            boolean match = true;

            if (isEqual != null) {
                XMLGregorianCalendar toCompareValue = DatatypeFactory.newInstance().newXMLGregorianCalendar(isEqual);
                match = toCompareValue.compare(value) == DatatypeConstants.EQUAL;
            }

            if (isBefore != null) {
                XMLGregorianCalendar toCompareValue = DatatypeFactory.newInstance().newXMLGregorianCalendar(isBefore);
                match = match && value.compare(toCompareValue) == DatatypeConstants.LESSER;
            }

            if (isAfter != null) {
                XMLGregorianCalendar toCompareValue = DatatypeFactory.newInstance().newXMLGregorianCalendar(isAfter);
                match = match && value.compare(toCompareValue) == DatatypeConstants.GREATER;
            }

            return match;
        } catch (Exception e) {
            return false;
        }
    }
}
