package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

public class LinearSubgraphFinder {
    public static List<List<EdgeToNode>> findAnywhere(INode startNode, List<BiPredicate<IEdge, INode>> filters) {
        List<List<EdgeToNode>> results = new ArrayList<>();

        if (startNode == null) {
            throw new IllegalArgumentException("Start node cannot be null");
        }

        BundleNodesTraverser.traverseAndExecute(startNode, node -> {
            List<List<EdgeToNode>> subgraphsFromNode = findFrom(node, filters);
            results.addAll(subgraphsFromNode);
        });

        return results;
    }

    public static List<List<EdgeToNode>> findFrom(INode startNode, List<BiPredicate<IEdge, INode>> filter) {
        List<List<EdgeToNode>> results = new ArrayList<>();

        recursiveFind(new EdgeToNode(null, startNode), new ArrayList<>(), filter, new HashSet<>(), results);

        return results;
    }

    private static void recursiveFind(EdgeToNode current, List<EdgeToNode> foundPart, List<BiPredicate<IEdge, INode>> specification, Set<IEdge> visited, List<List<EdgeToNode>> results) {
        if (current == null) return;
        if (foundPart == null) foundPart = new ArrayList<>();

        IEdge edge = current.edge;
        INode node = current.node;
        int index = foundPart.size();

        if (node == null) return;

        if (visited.contains(edge)) return;
        visited.add(edge);

        if (specification.size() <= index) return;

        if (!specification.get(index).test(edge, node)) return;

        foundPart.add(new EdgeToNode(edge, node));

        if (foundPart.size() == specification.size()) {
            results.add(foundPart);
            return;
        }

        for (IEdge e : node.getCauseEdges()) {
            recursiveFind(
                    new EdgeToNode(e, e.getEffect()),
                    new ArrayList<>(foundPart),
                    specification, new HashSet<>(visited), results);
        }
        for (IEdge e : node.getEffectEdges()) {
            recursiveFind(
                    new EdgeToNode(e, e.getCause()),
                    new ArrayList<>(foundPart),
                    specification, new HashSet<>(visited), results);
        }
    }
}
