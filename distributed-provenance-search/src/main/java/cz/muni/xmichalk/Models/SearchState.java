package cz.muni.xmichalk.Models;

import org.openprovenance.prov.model.QualifiedName;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class SearchState {
    public ConcurrentMap<QualifiedName, VisitedItem> visited;
    public ConcurrentMap<QualifiedName, ItemToSearch> processing;
    public PriorityBlockingQueue<ItemToSearch> toSearchQueue;
    public Set<FoundResult> results;

    public SearchState(ConcurrentMap<QualifiedName, VisitedItem> visited, ConcurrentMap<QualifiedName, ItemToSearch> processing, Map<QualifiedName, QualifiedName> connections, Map<QualifiedName, QualifiedName> pickedVersions, PriorityBlockingQueue<ItemToSearch> toSearchQueue, Set<FoundResult> results) {
        this.visited = visited;
        this.processing = processing;
        this.toSearchQueue = toSearchQueue;
        this.results = results;
    }
}
