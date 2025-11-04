package cz.muni.xmichalk.BundleVersionPicker;

import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.SpecifiedVersionPicker;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.Exceptions.UnsupportedVersionPreferrenceException;
import org.apache.commons.collections4.map.HashedMap;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;
import java.util.Map;

public class VersionPickerRegistry {
    private final Map<EVersionPreferrence, IVersionPicker> registry = new HashedMap<>();

    public VersionPickerRegistry(IDocumentLoader documentLoader) {
        registry.put(EVersionPreferrence.SPECIFIED, new SpecifiedVersionPicker());
        registry.put(EVersionPreferrence.LATEST, new LatestVersionPicker(documentLoader));
        // add more in future development
    }

    public VersionPickerRegistry(Map<EVersionPreferrence, IVersionPicker> registry) {
        this.registry.putAll(registry);
    }

    public IVersionPicker getVersionPicker(EVersionPreferrence versionPref) {
        return registry.get(versionPref);
    }

    public QualifiedName pickVersion(QualifiedName bundleId, EVersionPreferrence versionPreference) {
        IVersionPicker picker = registry.get(versionPreference);
        if (picker == null) {
            throw new UnsupportedVersionPreferrenceException("No version picker registered for version preference: " + versionPreference);
        }
        
        return picker.apply(bundleId);
    }

    public List<EVersionPreferrence> getAllVersionPreferrences() {

        return registry.keySet().stream().toList();
    }
}
