package cz.muni.xmichalk.models;

import org.openprovenance.prov.model.QualifiedName;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

public class TraversalState {
    public Set<QualifiedName> visitedPreferred;
    public Set<QualifiedName> visitedReferenced;
    public ConcurrentMap<QualifiedName, ItemToTraverse> traversingPreferred;
    public ConcurrentMap<QualifiedName, ItemToTraverse> traversingReferenced;
    public PriorityBlockingQueue<ItemToTraverse> toTraverseQueue;
    public ConcurrentLinkedQueue<ResultFromBundle> results;
    public ConcurrentLinkedQueue<String> errors;

    public TraversalState(Set<QualifiedName> visitedPreferred, Set<QualifiedName> visitedReferenced,
                          ConcurrentMap<QualifiedName, ItemToTraverse> traversingPreferred,
                          ConcurrentMap<QualifiedName, ItemToTraverse> traversingReferenced,
                          PriorityBlockingQueue<ItemToTraverse> toTraverseQueue,
                          ConcurrentLinkedQueue<ResultFromBundle> results, ConcurrentLinkedQueue<String> errors) {
        this.visitedPreferred = visitedPreferred;
        this.visitedReferenced = visitedReferenced;
        this.traversingPreferred = traversingPreferred;
        this.traversingReferenced = traversingReferenced;
        this.toTraverseQueue = toTraverseQueue;
        this.results = results;
        this.errors = errors;
    }
}
