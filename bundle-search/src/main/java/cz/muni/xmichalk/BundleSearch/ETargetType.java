package cz.muni.xmichalk.BundleSearch;

public enum ETargetType {
    NODE_IDS_BY_ID("Finds nodes with ids matching the given id, returns their ids. Expects target specification in the form of target id URI."),
    NODES_BY_ID("Finds nodes with ids matching the given id, returns the full nodes encapsulated in a prov document serialized as a JSON. Expects target specification in the form of target id URI."),
    NODE_IDS_BY_SPECIFICATION("Finds nodes fitting the specification, returns their ids."),
    NODES_BY_SPECIFICATION("Finds nodes fitting the specification, returns the full nodes encapsulated in a prov document serialized as a JSON."),
    CONNECTORS("Finds connectors matching the given specification, returns their relevant information in ConnectorDTO object. Expects target specification to be either 'backward' or 'forward'."),
    BUNDLE_ID_BY_META_BUNDLE_ID("If the searched bundles meta bundle fits the given one, returns the bundle id. Expects target specification in the form of meta bundle URI."),
    TEST_FITS("Test whether bundle fits the given requirements.");

    public String description;

    ETargetType(String description) {
        this.description = description;
    }
}