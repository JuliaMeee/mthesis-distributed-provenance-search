package cz.muni.xmichalk.bundleSearch;

public enum ETargetType {
    NODE_IDS("Finds nodes with ids matching the given condition, returns their ids. Expects target specification to be ICondition<INode>."),
    NODES("Finds nodes matching given condition, returns the full nodes encapsulated in a prov document serialized as a JSON. Expects target specification to be ICondition<INode>."),
    CONNECTORS("Finds connectors matching the given specification, returns their relevant information in ConnectorDTO object. Expects target specification to be either 'backward' or 'forward'."),
    TEST_FITS("Test whether bundle fits the given condition. Expects target specification to be ICondition<CpmDocument>.");

    public final String description;

    ETargetType(String description) {
        this.description = description;
    }
}