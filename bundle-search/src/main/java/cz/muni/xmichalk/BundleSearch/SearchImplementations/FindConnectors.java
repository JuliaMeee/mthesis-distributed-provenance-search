package cz.muni.xmichalk.BundleSearch.SearchImplementations;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.ISearchBundle;
import cz.muni.xmichalk.Models.ConnectorData;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.Objects;

public class FindConnectors implements ISearchBundle<List<ConnectorData>> {

    @Override
    public List<ConnectorData> apply(CpmDocument document, QualifiedName startNodeId, final JsonNode targetSpecification) {
        List<INode> connectorNodes;
        if (Objects.equals(targetSpecification.asText(), "backward")) {
            connectorNodes = document.getBackwardConnectors();
        } else {
            connectorNodes = document.getForwardConnectors();
        }

        return connectorNodes.isEmpty() ? null : connectorNodes.stream()
                .map(connectorNode -> new ConnectorData(connectorNode))
                .toList();
    }

}
