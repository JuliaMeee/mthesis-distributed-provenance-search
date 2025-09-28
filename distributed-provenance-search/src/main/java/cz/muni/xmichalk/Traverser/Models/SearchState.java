package cz.muni.xmichalk.Traverser.Models;

import org.openprovenance.prov.model.QualifiedName;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class SearchState {
    public ConcurrentMap<QualifiedName, VisitedItem> visited;
    public ConcurrentMap<QualifiedName, ItemToSearch> processing;
    public PriorityBlockingQueue<ItemToSearch> toSearch;
    public Set<FoundResult> results;

    public SearchState(ConcurrentMap<QualifiedName, VisitedItem> visited, ConcurrentMap<QualifiedName, ItemToSearch> processing, PriorityBlockingQueue<ItemToSearch> toSearch, Set<FoundResult> results) {
        this.visited = visited;
        this.processing = processing;
        this.toSearch = toSearch;
        this.results = results;
    }
}
