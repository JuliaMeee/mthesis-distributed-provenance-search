package cz.muni.xmichalk.BundleSearch.General;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.*;

import java.util.*;
import java.util.function.Predicate;

public final class BundleNodesSearcher {
    public static List<INode> search(CpmDocument document, QualifiedName startNodeId, Predicate<INode> filter) {
        Set<INode> visited = new HashSet<>();
        Queue<INode> queue = new ArrayDeque<>();
        List<INode> results = new ArrayList<>();

        INode start = document.getNode(startNodeId);
        if (start == null) {
            throw new IllegalArgumentException("Start node with id " + startNodeId.getUri() + " does not exist in document " + document.getBundleId().getUri());
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
}
