package cz.muni.xmichalk.targetSpecification.attributeSpecification;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.util.CpmUtils;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class TimestampAttrSpecification extends AttrSpecification {
    public String isEqual;
    public String isBefore;
    public String isAfter;

    public TimestampAttrSpecification() {
    }

    public TimestampAttrSpecification(String attributeNameUri, String isEqual, String isBefore, String isAfter) {
        this.attributeNameUri = attributeNameUri;
        this.isEqual = isEqual;
        this.isBefore = isBefore;
        this.isAfter = isAfter;
    }

    public boolean test(INode node) {
        try {
            XMLGregorianCalendar value = (XMLGregorianCalendar) CpmUtils.getAttributeValue(node, attributeNameUri);
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
