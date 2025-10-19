package cz.muni.xmichalk.BundleSearch.SearchImplementations;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.BundleSearch.ISearchBundle;
import cz.muni.xmichalk.DTO.ConnectorDTO;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.Objects;

public class FindConnectors implements ISearchBundle<List<ConnectorDTO>> {
    
    @Override
    public List<ConnectorDTO> apply(CpmDocument document, QualifiedName startNodeId, final String targetSpecification) {
        List<INode> connectorNodes;
        if (Objects.equals(targetSpecification, "backward")) {
            connectorNodes = document.getBackwardConnectors();
        }
        else {
            connectorNodes = document.getForwardConnectors();
        }
        
        return connectorNodes.isEmpty() ? null : connectorNodes.stream()
                .map(connectorNode -> new ConnectorDTO(connectorNode))
                .toList();
    }
    
}
