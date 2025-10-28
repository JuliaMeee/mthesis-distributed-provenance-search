package cz.muni.xmichalk.BundleVersionPicker;

import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.SpecifiedVersionPicker;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import org.apache.commons.collections4.map.HashedMap;

import java.util.List;
import java.util.Map;

public class VersionPickerRegistry {
    private final Map<EVersionPreferrence, IVersionPicker> registry = new HashedMap<>();
    
    public VersionPickerRegistry(IDocumentLoader documentLoader) {
        registry.put(EVersionPreferrence.SPECIFIED, new SpecifiedVersionPicker());
        registry.put(EVersionPreferrence.LATEST, new LatestVersionPicker(documentLoader));
        // add more in future development
    }

    public VersionPickerRegistry(Map<EVersionPreferrence, IVersionPicker> registry){
        this.registry.putAll(registry);
    }
    
    public IVersionPicker getVersionPicker(EVersionPreferrence versionPref) {
        return registry.get(versionPref);
    }

    public List<EVersionPreferrence> getAllVersionPreferrences() {
        
        return registry.keySet().stream().toList();
    }
}
