package cz.muni.xmichalk.bundleVersionPicker;

import org.openprovenance.prov.model.QualifiedName;

public interface IVersionPicker {
    QualifiedName apply(QualifiedName bundleId);
}
