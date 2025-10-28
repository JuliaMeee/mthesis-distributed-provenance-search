package cz.muni.xmichalk.BundleSearch.General;

import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.Util.Pair;

import java.util.*;
import java.util.function.BiPredicate;

public class LinearSubgraphFinder {
    public static List<List<Pair<IEdge, INode>>> findAnywhere(INode startNode, List<BiPredicate<IEdge, INode>> filters) {
        
        Set<INode> visited = new HashSet<>();
        Queue<INode> queue = new ArrayDeque<>();
        List<List<Pair<IEdge, INode>>> results = new ArrayList<>();

        if (startNode == null) {
            throw new IllegalArgumentException("Start node cannot be null");
        }

        queue.add(startNode);

        while (!queue.isEmpty()) {
            INode node =queue.remove();
            
            if (node == null || visited.contains(node)) continue;

            visited.add(node);
            
            var subgraphsFromNode = findFrom(node, filters);
            results.addAll(subgraphsFromNode);

            for (IEdge e : node.getCauseEdges()) {
                queue.add(e.getEffect());
            }
            for (IEdge e : node.getEffectEdges()) {
                queue.add(e.getCause());
            }
        }

        return results;
    }
    
    public static List<List<Pair<IEdge, INode>>> findFrom(INode startNode, List<BiPredicate<IEdge, INode>> filter) {
        List<List<Pair<IEdge, INode>>> results = new ArrayList<>();
        
        recursiveSearch(new Pair<>(null, startNode), new ArrayList<>(), filter, new HashSet<>(), results);
        
        return results;
    }
    
    private static void recursiveSearch(Pair<IEdge, INode> current, List<Pair<IEdge, INode>> foundPart, List<BiPredicate<IEdge, INode>> constraints, Set<INode> visited, List<List<Pair<IEdge, INode>>> results) {
        if (current == null) return;
        if (foundPart == null) foundPart = new ArrayList<>();
        
        IEdge edge = current.first();
        INode node = current.second();
        Integer index = foundPart.size();
        
        if (node == null) return;
        
        if (visited.contains(node)) return;
        visited.add(node);
        
        if (constraints.size() <= index) return;

        if (!constraints.get(index).test(edge, node)) return;

        foundPart.add(new Pair<>(edge, node));

        if (foundPart.size() == constraints.size()) {
            results.add(foundPart);
            return;
        }

        for (IEdge e : node.getCauseEdges()) {
            recursiveSearch(
                    new Pair<>(e, e.getEffect()),
                    new ArrayList<>(foundPart),
                    constraints, new HashSet<>(visited), results);
        }
        for (IEdge e : node.getEffectEdges()) {
            recursiveSearch(
                    new Pair<>(e, e.getCause()),
                    new ArrayList<>(foundPart),
                    constraints, new HashSet<>(visited), results);
        }
    }
}
