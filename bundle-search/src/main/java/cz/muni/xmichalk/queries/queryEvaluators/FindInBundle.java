package cz.muni.xmichalk.queries.queryEvaluators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;

import java.util.List;
import java.util.function.Function;

public class FindInBundle<R, T> implements IQueryEvaluator<T> {
    private final Function<List<R>, T> resultTransformation;

    public FindInBundle(Function<List<R>, T> resultTransformation) {
        this.resultTransformation = resultTransformation;
    }

    @Override
    public T apply(CpmDocument document, org.openprovenance.prov.model.QualifiedName startNodeId, JsonNode querySpecification) {
        ObjectMapper objectMapper = new ObjectMapper();

        IFindableInDocument<R> findable = objectMapper.convertValue(querySpecification, new TypeReference<IFindableInDocument<R>>() {
        });

        INode startNode = document.getNode(startNodeId);
        if (startNode == null) {
            throw new IllegalArgumentException("Start node with id " + startNodeId.getUri() + " does not exist in document " + document.getBundleId().getUri());
        }

        List<R> foundItems = findable.find(startNode);

        return resultTransformation.apply(foundItems);
    }
}
