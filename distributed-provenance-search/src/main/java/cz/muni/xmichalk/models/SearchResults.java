package cz.muni.xmichalk.models;

import java.util.Collection;

public class SearchResults {
    public Collection<FoundResult> results;
    public Collection<String> errors;

    public SearchResults(Collection<FoundResult> results, Collection<String> errors) {
        this.results = results;
        this.errors = errors;
    }
}
