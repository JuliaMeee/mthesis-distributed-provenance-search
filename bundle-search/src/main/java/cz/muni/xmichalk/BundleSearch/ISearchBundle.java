package cz.muni.xmichalk.BundleSearch;

import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.QualifiedName;

@FunctionalInterface
public interface ISearchBundle<T> {
    T apply(CpmDocument document, QualifiedName startNodeId, String targetSpecification);
}
