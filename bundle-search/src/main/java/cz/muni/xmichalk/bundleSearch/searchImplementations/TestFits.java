package cz.muni.xmichalk.bundleSearch.searchImplementations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.bundleSearch.ISearchBundle;
import cz.muni.xmichalk.targetSpecification.ICondition;

public class TestFits implements ISearchBundle<Boolean> {
    @Override
    public Boolean apply(CpmDocument document, org.openprovenance.prov.model.QualifiedName startNodeId, JsonNode targetSpecification) {
        ObjectMapper mapper = new ObjectMapper();
        ICondition<CpmDocument> requirement = mapper.convertValue(targetSpecification,
                new TypeReference<ICondition<CpmDocument>>() {
                });
        return requirement.test(document);
    }
}
