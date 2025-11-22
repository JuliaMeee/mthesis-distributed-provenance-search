package cz.muni.xmichalk.queries;

public enum EQueryType {
    NODE_IDS("Finds nodes with ids matching the given condition, returns their ids. Expects query specification to be ICondition<INode>."),
    NODES("Finds nodes matching given condition, returns the full nodes encapsulated in a prov document serialized as a JSON. Expects query specification to be ICondition<INode>."),
    SUBGRAPHS("Finds subgraphs matching the given specification, returns the full subgraphs encapsulated in a prov document serialized as a JSON. Expects query specification to be List<ICondition<EdgeToNode>>."),
    CONNECTORS("Finds connectors matching the given specification, returns their relevant information in ConnectorDTO object. Expects query specification to be either 'backward' or 'forward'."),
    TEST_FITS("Test whether bundle fits the given condition. Expects query specification to be ICondition<CpmDocument>.");

    public final String description;

    EQueryType(String description) {
        this.description = description;
    }
}