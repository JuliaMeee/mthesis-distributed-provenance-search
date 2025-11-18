package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.QualifiedName;

@FunctionalInterface
public interface IQueryEvaluator<T> {
    T apply(CpmDocument document, QualifiedName startNodeId, JsonNode querySpecification);
}
