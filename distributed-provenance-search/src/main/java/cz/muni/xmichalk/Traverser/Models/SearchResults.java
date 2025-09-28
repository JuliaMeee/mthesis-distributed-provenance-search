package cz.muni.xmichalk.Traverser.Models;

import java.util.List;

public class SearchResults {
    public List<FoundResult> results;

    public SearchResults(List<FoundResult> results) {
        this.results = results;
    }
}
