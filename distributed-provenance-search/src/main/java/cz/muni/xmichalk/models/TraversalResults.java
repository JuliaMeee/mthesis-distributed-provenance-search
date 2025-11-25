package cz.muni.xmichalk.models;

import java.util.Collection;

public class TraversalResults {
    public Collection<ResultFromBundle> results;
    public Collection<String> errors;

    public TraversalResults(Collection<ResultFromBundle> results, Collection<String> errors) {
        this.results = results;
        this.errors = errors;
    }
}
