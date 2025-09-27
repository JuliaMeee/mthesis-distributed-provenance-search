package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.stream.Collectors;

public class SearchBundleResult {
    public List<QualifiedName> results;
    public List<ConnectorNode> connectors;
    public ECredibility credibility;

    public SearchBundleResult() {
    }

    public SearchBundleResult(List<INode> results, List<INode> connectors, ECredibility credibility) {
        this.results = results.stream().map(x -> x.getId()).collect(Collectors.toList());
        this.connectors = connectors.stream().map(x -> new ConnectorNode(x)).collect(Collectors.toList());
        this.credibility = credibility;
    }
}
