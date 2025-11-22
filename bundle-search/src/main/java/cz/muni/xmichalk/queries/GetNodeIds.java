package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;

import java.util.List;

public class GetNodeIds implements IQuery<List<QualifiedNameData>> {
    public IFindableInDocument<INode> nodeFinder;

    public GetNodeIds() {
    }

    public GetNodeIds(IFindableInDocument<INode> nodeFinder) {
        this.nodeFinder = nodeFinder;
    }

    @Override
    public List<QualifiedNameData> evaluate(CpmDocument document, INode startNode) {
        if (nodeFinder == null) {
            return null;
        }
        List<INode> foundNodes = nodeFinder.find(document, startNode);

        return transformNodesToIds(foundNodes);
    }

    public List<QualifiedNameData> transformNodesToIds(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        return nodes.stream()
                .map(node -> new QualifiedNameData().from(node.getId()))
                .toList();
    }
}
