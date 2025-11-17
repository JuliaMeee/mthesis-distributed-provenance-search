package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class BundleNodesTraverser {
    public static List<INode> traverseAndFind(CpmDocument document, QualifiedName startNodeId, Predicate<INode> filter) {
        List<INode> results = new ArrayList<>();

        INode start = document.getNode(startNodeId);
        if (start == null) {
            throw new IllegalArgumentException("Start node with id " + startNodeId.getUri() + " does not exist in document " + document.getBundleId().getUri());
        }

        traverseAndExecute(start, node -> {
            if (filter.test(node)) {
                results.add(node);
            }
        });

        return results;
    }

    public static List<INode> traverseAndFind(INode start, Predicate<INode> filter) {
        List<INode> results = new ArrayList<>();

        traverseAndExecute(start, node -> {
            if (filter.test(node)) {
                results.add(node);
            }
        });

        return results;
    }

    public static void traverseAndExecute(INode start, Consumer<INode> action) {
        Set<INode> visited = new HashSet<>();
        Queue<INode> queue = new ArrayDeque<>();

        queue.add(start);

        while (!queue.isEmpty()) {
            INode current = queue.poll();
            if (current == null || visited.contains(current)) continue;

            visited.add(current);

            action.accept(current);

            for (IEdge e : current.getCauseEdges()) {
                queue.add(e.getEffect());
            }
            for (IEdge e : current.getEffectEdges()) {
                queue.add(e.getCause());
            }
        }
    }
}
