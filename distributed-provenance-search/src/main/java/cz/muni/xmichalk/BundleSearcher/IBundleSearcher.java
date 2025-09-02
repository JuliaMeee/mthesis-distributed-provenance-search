package cz.muni.xmichalk.BundleSearcher;


import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.function.Predicate;

public interface IBundleSearcher {
    List<INode> search(CpmDocument doc, QualifiedName startNodeId, Predicate<INode> predicate);
}
