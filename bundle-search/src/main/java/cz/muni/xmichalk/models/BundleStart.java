package cz.muni.xmichalk.models;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;

public class BundleStart {
    public CpmDocument bundle;
    public INode startNode;

    public BundleStart() {
    }

    public BundleStart(CpmDocument bundle, INode startNode) {
        this.bundle = bundle;
        this.startNode = startNode;
    }
}
