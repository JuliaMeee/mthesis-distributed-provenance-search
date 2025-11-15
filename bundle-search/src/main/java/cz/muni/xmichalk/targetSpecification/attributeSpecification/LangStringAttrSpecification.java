package cz.muni.xmichalk.targetSpecification.attributeSpecification;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.util.CpmUtils;
import org.openprovenance.prov.model.LangString;

public class LangStringAttrSpecification extends AttrSpecification {
    public String langRegex;
    public String valueRegex;

    public LangStringAttrSpecification() {
    }

    public LangStringAttrSpecification(String attributeNameUri, String langRegex, String valueRegex) {
        this.attributeNameUri = attributeNameUri;
        this.langRegex = langRegex;
        this.valueRegex = valueRegex;
    }

    public boolean test(INode node) {
        try {
            return CpmUtils.hasAttributeTargetValue(node, attributeNameUri, LangString.class, (langString)
                    -> {
                boolean langMatch = langRegex == null || langString.getLang().matches(langRegex);
                boolean valueMatch = valueRegex == null || langString.getValue().matches(valueRegex);
                return langMatch && valueMatch;
            });
        } catch (Exception e) {
            return false;
        }
    }

}
