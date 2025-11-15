package cz.muni.xmichalk.models;

import cz.muni.xmichalk.bundleVersionPicker.EVersionPreferrence;

public record PickVersionParams(QualifiedNameData bundleId, EVersionPreferrence versionPreference) {
}

