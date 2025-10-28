package cz.muni.xmichalk.Models;

import cz.muni.xmichalk.BundleVersionPicker.EVersionPreferrence;

public record PickVersionParams(QualifiedNameData bundleId, EVersionPreferrence versionPreference) {
}

