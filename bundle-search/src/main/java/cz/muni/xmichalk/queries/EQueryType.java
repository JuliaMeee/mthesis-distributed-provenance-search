package cz.muni.xmichalk.queries;

public enum EQueryType {
    NODE_IDS("Finds fitting nodes, returns list of node ids, each id consisting of nameSpaceUri and localPart. Returns null if no fitting nodes were found. Expects query specification to be IFindableInDocument<INode>."),
    NODES("Finds fitting nodes, returns a singe json-serialized prov document encapsulating all the nodes. Returns null if no fitting nodes were found. Expects query specification to be IFindableInDocument<INode>."),
    SUBGRAPHS("Finds fitting subgraphs, returns a list of json-serialized prov documents, each document encapsulating a single subgraph. Returns null if no fitting subgraphs were found. Expects query specification to be IFindableInDocument<List<EdgeToNode>>."),
    CONNECTORS("Finds connectors in the given direction, returns their relevant information in ConnectorDTO object. Expects query specification to be either 'backward' or 'forward'."),
    TEST_FITS("Tests whether bundle fits the given condition, returns a boolean. Expects query specification to be ICondition<CpmDocument>.");

    public final String description;

    EQueryType(String description) {
        this.description = description;
    }
}