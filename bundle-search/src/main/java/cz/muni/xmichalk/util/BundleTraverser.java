package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BundleTraverser {

    public static List<INode> traverseAndFindNodes(INode startNode, Predicate<INode> nodePredicate, Predicate<EdgeToNode> pathFilter) {
        List<INode> results = new java.util.ArrayList<>();

        traverseFrom(startNode, (edgeToNode -> {
            if (nodePredicate.test(edgeToNode.node)) {
                results.add(edgeToNode.node);
            }
        }), pathFilter);

        return results;
    }

    public static void traverseFrom(INode startNode, Consumer<EdgeToNode> traversedConsumer, Predicate<EdgeToNode> pathFilter) {
        traverseRecursive(new EdgeToNode(null, startNode), pathFilter, new HashSet<>(), traversedConsumer);
    }

    private static void traverseRecursive(EdgeToNode current, Predicate<EdgeToNode> pathFilter, Set<INode> visited, Consumer<EdgeToNode> traversedConsumer) {
        if (current == null) return;

        IEdge edge = current.edge;
        INode node = current.node;

        if (node == null) return;

        if (visited.contains(node)) return;

        if (edge != null) { // skip for first node
            if (pathFilter != null && !pathFilter.test(current)) return;
        }


        visited.add(node);

        traversedConsumer.accept(current);

        for (IEdge e : node.getCauseEdges()) {
            traverseRecursive(
                    new EdgeToNode(e, e.getEffect()),
                    pathFilter,
                    visited,
                    traversedConsumer);
        }
        for (IEdge e : node.getEffectEdges()) {
            traverseRecursive(
                    new EdgeToNode(e, e.getCause()),
                    pathFilter,
                    visited,
                    traversedConsumer);
        }
    }
}
