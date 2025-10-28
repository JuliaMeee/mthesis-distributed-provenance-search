package cz.muni.xmichalk.BundleVersionPicker;

import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.QualifiedName;

public interface IVersionPicker {
    public QualifiedName apply(QualifiedName bundleId);
}
