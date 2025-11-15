package cz.muni.xmichalk.models;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;

public record EdgeToNode(IEdge edge, INode node) {
}