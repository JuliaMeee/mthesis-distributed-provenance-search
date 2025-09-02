package cz.muni.xmichalk.Traverser.Models;

import cz.muni.fi.cpm.model.INode;

import java.util.List;

public class ProcessBundleResult {
    public List<INode> results;
    public List<INode> connectors;
    public ECredibility credibility;

    public ProcessBundleResult(List<INode> results, List<INode> connectors, ECredibility credibility) {
        this.results = results;
        this.connectors = connectors;
        this.credibility = credibility;
    }
}
