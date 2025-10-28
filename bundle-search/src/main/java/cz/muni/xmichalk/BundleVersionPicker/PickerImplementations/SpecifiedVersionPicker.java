package cz.muni.xmichalk.BundleVersionPicker.PickerImplementations;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.BundleVersionPicker.IVersionPicker;
import org.openprovenance.prov.model.QualifiedName;

public class SpecifiedVersionPicker implements IVersionPicker {

    @Override
    public QualifiedName apply(final QualifiedName bundleId) {
        return bundleId;
    }
}
