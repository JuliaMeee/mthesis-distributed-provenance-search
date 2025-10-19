package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

public class FoundResult {
    public QualifiedName bundleId;
    public Object result;
    public boolean pathIntegrity;
    public boolean integrity;
    public boolean pathValidity;
    public boolean validity;


    public FoundResult() {

    }

    public FoundResult(QualifiedName bundleId, Object result, boolean pathIntegrity, boolean integrity, boolean pathValidity, boolean validity) {
        this.bundleId = bundleId;
        this.result = result;
        this.pathIntegrity = pathIntegrity;
        this.integrity = integrity;
        this.pathValidity = pathValidity;
        this.validity = validity;
    }

    public FoundResult(QualifiedName bundleId, Object result) {
        this.bundleId = bundleId;
        this.result = result;
    }
}
