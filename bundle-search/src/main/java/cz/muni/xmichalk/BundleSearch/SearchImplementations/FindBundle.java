package cz.muni.xmichalk.BundleSearch.SearchImplementations;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.ISearchBundle;
import cz.muni.xmichalk.Util.CpmUtils;
import org.openprovenance.prov.model.QualifiedName;

import java.util.function.Function;
import java.util.function.Predicate;

import static cz.muni.xmichalk.Util.AttributeNames.*;

public class FindBundle<T> implements ISearchBundle<T> {
    private final Function<JsonNode, Predicate<CpmDocument>> translatePredicate;
    private final Function<CpmDocument, T> resultTransformation;

    public FindBundle(Function<JsonNode, Predicate<CpmDocument>> getPredicate, Function<CpmDocument, T> resultTransformation) {
        this.translatePredicate = getPredicate;
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(final CpmDocument document, final QualifiedName startNodeId, final JsonNode targetSpecification) {
        var predicate = translatePredicate.apply(targetSpecification);
        var results = predicate.test(document) ? document : null;
        return resultTransformation.apply(results);
    }
    
    public static Predicate<CpmDocument> translateMetaBundleIdToPredicate(JsonNode targetSpecification) {
        return (CpmDocument doc) -> {
            INode mainActivity = doc.getMainActivity();
            if (mainActivity != null) {
                var value = CpmUtils.getAttributeValue(mainActivity, ATTR_REFERENCED_META_BUNDLE_ID);
                if (value instanceof QualifiedName qnValue) {
                    return targetSpecification.asText().equals(qnValue.getUri());
                }
            }
            return false;
        };
    }
}
