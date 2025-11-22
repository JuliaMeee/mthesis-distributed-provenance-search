package cz.muni.xmichalk.queries.queryEvaluators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import cz.muni.xmichalk.querySpecification.ICondition;

public class TestFits implements IQueryEvaluator<Boolean> {
    @Override
    public Boolean apply(CpmDocument document, org.openprovenance.prov.model.QualifiedName startNodeId, JsonNode querySpecification) {
        ObjectMapper mapper = new ObjectMapper();
        ICondition<CpmDocument> requirement = mapper.convertValue(querySpecification,
                new TypeReference<ICondition<CpmDocument>>() {
                });
        return requirement.test(document);
    }
}
