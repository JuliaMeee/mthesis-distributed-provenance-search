package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;

import java.util.List;

public class SearchBundleResultDTO {
    public List<QualifiedNameDTO> resultIds;
    public List<ConnectorDataDTO> connectors;
    public ECredibility credibility;

    public SearchBundleResultDTO() {
    }

    public SearchBundleResultDTO(List<INode> results, List<INode> connectors, ECredibility credibility) {
        this.resultIds = results.stream().map(node -> new QualifiedNameDTO(node.getId())).toList();
        this.connectors = connectors.stream().map(node -> new ConnectorDataDTO(node)).toList();
        this.credibility = credibility;
    }
}
