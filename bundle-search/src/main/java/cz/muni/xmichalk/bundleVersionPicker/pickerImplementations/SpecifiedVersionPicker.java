package cz.muni.xmichalk.bundleVersionPicker.pickerImplementations;

import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import org.openprovenance.prov.model.QualifiedName;

public class SpecifiedVersionPicker implements IVersionPicker {

    @Override
    public QualifiedName apply(QualifiedName bundleId) {
        return bundleId;
    }
}
