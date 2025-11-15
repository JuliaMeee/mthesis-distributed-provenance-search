package cz.muni.xmichalk.models;

import org.openprovenance.prov.model.QualifiedName;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class SearchState {
    public ConcurrentMap<QualifiedName, VisitedItem> visited;
    public ConcurrentMap<QualifiedName, ItemToSearch> processing;
    public PriorityBlockingQueue<ItemToSearch> toSearchQueue;
    public ConcurrentLinkedQueue<FoundResult> results;
    public ConcurrentLinkedQueue<String> errors;

    public SearchState(ConcurrentMap<QualifiedName, VisitedItem> visited, ConcurrentMap<QualifiedName, ItemToSearch> processing, PriorityBlockingQueue<ItemToSearch> toSearchQueue, ConcurrentLinkedQueue<FoundResult> results, ConcurrentLinkedQueue<String> errors) {
        this.visited = visited;
        this.processing = processing;
        this.toSearchQueue = toSearchQueue;
        this.results = results;
        this.errors = errors;
    }
}
