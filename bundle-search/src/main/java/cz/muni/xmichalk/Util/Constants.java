package cz.muni.xmichalk.Util;

import org.openprovenance.prov.vanilla.QualifiedName;

public class Constants {
    public static final String CPM_URI = "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/";
    public static final String PROV_URI = "http://www.w3.org/ns/prov#";
    public static final String BLANK_URI = "https://openprovenance.org/blank/";
    
    public static final QualifiedName ATTR_REFERENCED_BUNDLE_ID = new QualifiedName(CPM_URI, "referencedBundleId", "cpm");
    public static final QualifiedName ATTR_REFERENCED_META_BUNDLE_ID = new QualifiedName(CPM_URI, "referencedMetaBundleId", "cpm");
    public static final QualifiedName ATTR_REFERENCED_BUNDLE_HASH_VALUE = new QualifiedName(CPM_URI, "referencedBundleHashValue", "cpm");
    public static final QualifiedName ATTR_HASH_ALG = new QualifiedName(CPM_URI, "hashAlg", "cpm");
}
