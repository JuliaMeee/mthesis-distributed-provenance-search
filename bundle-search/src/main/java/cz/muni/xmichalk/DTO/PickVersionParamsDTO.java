package cz.muni.xmichalk.DTO;

import cz.muni.xmichalk.BundleVersionPicker.EVersionPreferrence;

public record PickVersionParamsDTO (QualifiedNameDTO bundleId, EVersionPreferrence versionPreference){}

