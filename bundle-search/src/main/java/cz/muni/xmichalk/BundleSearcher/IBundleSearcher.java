package cz.muni.xmichalk.BundleSearcher;


import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.QualifiedName;

public interface IBundleSearcher<T, S> {
    T search(CpmDocument doc, QualifiedName startNodeId);
    
    S serializeResult(T result);
}
