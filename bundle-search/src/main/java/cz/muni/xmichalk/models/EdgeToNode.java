package cz.muni.xmichalk.models;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;

public class EdgeToNode {
    public IEdge edge;
    public INode node;

    public EdgeToNode() {
    }

    public EdgeToNode(IEdge edge, INode node) {
        this.edge = edge;
        this.node = node;
    }
}