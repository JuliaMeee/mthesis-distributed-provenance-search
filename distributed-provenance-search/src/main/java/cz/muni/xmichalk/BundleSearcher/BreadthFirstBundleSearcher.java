package cz.muni.xmichalk.BundleSearcher;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

import java.util.*;
import java.util.function.Predicate;

public final class BreadthFirstBundleSearcher implements IBundleSearcher {

    @Override
    public List<INode> search(CpmDocument doc, QualifiedName startNodeId, Predicate<INode> predicate) {
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

            if (predicate.test(current)) {
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
}
