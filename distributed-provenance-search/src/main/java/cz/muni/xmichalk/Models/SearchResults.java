package cz.muni.xmichalk.Models;

import java.util.List;

public class SearchResults {
    public List<FoundResult> results;

    public SearchResults(List<FoundResult> results) {
        this.results = results;
    }
}
