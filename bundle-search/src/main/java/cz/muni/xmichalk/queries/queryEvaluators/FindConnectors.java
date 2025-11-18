package cz.muni.xmichalk.queries.queryEvaluators;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.Objects;

public class FindConnectors implements IQueryEvaluator<List<ConnectorData>> {

    @Override
    public List<ConnectorData> apply(CpmDocument document, QualifiedName startNodeId, JsonNode querySpecification) {
        List<INode> connectorNodes;
        if (Objects.equals(querySpecification.asText(), "backward")) {
            connectorNodes = document.getBackwardConnectors();
        } else {
            connectorNodes = document.getForwardConnectors();
        }

        return connectorNodes.isEmpty() ? null : connectorNodes.stream()
                .map(connectorNode -> new ConnectorData(connectorNode))
                .toList();
    }

}
