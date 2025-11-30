package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class LinearSubgraphFinder {
    public static List<List<EdgeToNode>> findSubgraphs(INode startNode, List<Predicate<EdgeToNode>> subgraphSpecification, Predicate<EdgeToNode> pathFilter) {
        List<List<EdgeToNode>> results = new ArrayList<>();

        if (startNode == null) {
            throw new IllegalArgumentException("Start node cannot be null");
        }

        BundleTraverser.traverseFrom(startNode, edgeToNode -> {
            List<List<EdgeToNode>> subgraphsFromNode = findSubgraphsFrom(edgeToNode.node, subgraphSpecification);
            results.addAll(subgraphsFromNode);
        }, pathFilter);

        return results;
    }

    public static List<List<EdgeToNode>> findSubgraphsFrom(INode startNode, List<Predicate<EdgeToNode>> subgraphSpecification) {
        List<List<EdgeToNode>> results = new ArrayList<>();

        recursiveFindSubgraph(new EdgeToNode(null, startNode), new ArrayList<>(), subgraphSpecification, new HashSet<>(), results);

        return results;
    }

    private static void recursiveFindSubgraph(EdgeToNode current, List<EdgeToNode> foundPart, List<Predicate<EdgeToNode>> specification, Set<IEdge> visited, List<List<EdgeToNode>> results) {
        if (current == null) return;
        if (foundPart == null) foundPart = new ArrayList<>();

        IEdge edge = current.edge;
        INode node = current.node;
        int index = foundPart.size();

        if (node == null) return;

        if (visited.contains(edge)) return;
        visited.add(edge);

        if (specification.size() <= index) return;

        if (!specification.get(index).test(current)) return;

        foundPart.add(current);

        if (foundPart.size() == specification.size()) {
            results.add(foundPart);
            return;
        }

        for (IEdge e : node.getCauseEdges()) {
            recursiveFindSubgraph(
                    new EdgeToNode(e, e.getEffect()),
                    new ArrayList<>(foundPart),
                    specification, new HashSet<>(visited), results);
        }
        for (IEdge e : node.getEffectEdges()) {
            recursiveFindSubgraph(
                    new EdgeToNode(e, e.getCause()),
                    new ArrayList<>(foundPart),
                    specification, new HashSet<>(visited), results);
        }
    }
}
