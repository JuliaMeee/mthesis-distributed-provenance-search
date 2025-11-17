package cz.muni.xmichalk.models;

import cz.muni.xmichalk.bundleVersionPicker.EVersionPreferrence;

public class PickVersionParams {
    public QualifiedNameData bundleId;
    public EVersionPreferrence versionPreference;

    public PickVersionParams() {
    }

    public PickVersionParams(QualifiedNameData bundleId, EVersionPreferrence versionPreference) {
        this.bundleId = bundleId;
        this.versionPreference = versionPreference;
    }
}