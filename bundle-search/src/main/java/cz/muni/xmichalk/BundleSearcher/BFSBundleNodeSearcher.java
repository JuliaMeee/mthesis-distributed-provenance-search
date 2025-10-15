package cz.muni.xmichalk.BundleSearcher;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

import java.util.*;
import java.util.function.Predicate;

public final class BFSBundleNodeSearcher implements IBundleSearcher<List<INode>, List<String>> {

    private final Predicate<INode> filter;

    public BFSBundleNodeSearcher(Predicate<INode> filter) {
        this.filter = filter;
    }

    @Override
    public List<INode> search(CpmDocument doc, QualifiedName startNodeId) {
        Set<INode> visited = new HashSet<>();
        Queue<INode> queue = new ArrayDeque<>();
        List<INode> results = new ArrayList<>();

        INode start = doc.getNode(startNodeId);
        if (start == null) {
            // TODO throw an error or return a message?
            // or do queue.addAll(doc.getNodes()); and continue?
            return results;
        }

        queue.add(start);

        while (!queue.isEmpty()) {
            INode current = queue.poll();
            if (current == null || visited.contains(current)) continue;

            visited.add(current);

            if (filter.test(current)) {
                results.add(current);
            }

            for (IEdge e : current.getCauseEdges()) {
                queue.add(e.getEffect());
            }
            for (IEdge e : current.getEffectEdges()) {
                queue.add(e.getCause());
            }
        }

        return results;
    }
    
    @Override
    public List<String> serializeResult(List<INode> result) {
        List<String> serialized = new ArrayList<>();
        for (INode node : result) {
            serialized.add(node.getId().getUri());
        }
        return serialized;
    }
}
