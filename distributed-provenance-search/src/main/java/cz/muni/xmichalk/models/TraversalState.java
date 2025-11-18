package cz.muni.xmichalk.models;

import org.openprovenance.prov.model.QualifiedName;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class TraversalState {
    public ConcurrentMap<QualifiedName, VisitedItem> visited;
    public ConcurrentMap<QualifiedName, ItemToTraverse> processing;
    public PriorityBlockingQueue<ItemToTraverse> toTraverseQueue;
    public ConcurrentLinkedQueue<ResultFromBundle> results;
    public ConcurrentLinkedQueue<String> errors;

    public TraversalState(ConcurrentMap<QualifiedName, VisitedItem> visited, ConcurrentMap<QualifiedName, ItemToTraverse> processing, PriorityBlockingQueue<ItemToTraverse> toTraverseQueue, ConcurrentLinkedQueue<ResultFromBundle> results, ConcurrentLinkedQueue<String> errors) {
        this.visited = visited;
        this.processing = processing;
        this.toTraverseQueue = toTraverseQueue;
        this.results = results;
        this.errors = errors;
    }
}
