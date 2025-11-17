package cz.muni.xmichalk.bundleVersionPicker;

import cz.muni.xmichalk.bundleVersionPicker.pickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.pickerImplementations.SpecifiedVersionPicker;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;

import java.util.Map;

public class VersionPickerProvider {
    public static Map<EVersionPreferrence, IVersionPicker> getVersionPickers(IDocumentLoader documentLoader) {
        return Map.of(
                EVersionPreferrence.SPECIFIED, new SpecifiedVersionPicker(),
                EVersionPreferrence.LATEST, new LatestVersionPicker(documentLoader)
        );
    }
}
