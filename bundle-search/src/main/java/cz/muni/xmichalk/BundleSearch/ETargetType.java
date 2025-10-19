package cz.muni.xmichalk.BundleSearch;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ETargetType {
    NODE_IDS_BY_ID,
    NODE_IDS_BY_ATTRIBUTES,
    NODES_BY_ATTRIBUTES,
    CONNECTORS,
    BUNDLE_ID_BY_META_BUNDLE_ID;
}