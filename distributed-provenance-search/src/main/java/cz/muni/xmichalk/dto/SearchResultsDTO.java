package cz.muni.xmichalk.dto;

import cz.muni.xmichalk.models.SearchResults;

import java.util.ArrayList;
import java.util.Collection;

public class SearchResultsDTO implements IDTO<SearchResults> {
    public Collection<FoundResultDTO> results;
    public Collection<String> errors;

    public SearchResultsDTO() {
    }

    public SearchResultsDTO(Collection<FoundResultDTO> results, Collection<String> errors) {
        this.results = results;
        this.errors = errors;
    }

    @Override
    public SearchResults toDomainModel() {
        return new SearchResults(
                this.results.stream().map(FoundResultDTO::toDomainModel).toList(),
                new ArrayList<>(this.errors)
        );
    }

    @Override
    public SearchResultsDTO from(SearchResults domainModel) {
        if (domainModel == null) {
            return null;
        }
        this.results = domainModel.results.stream().map(innerNode -> new FoundResultDTO().from(innerNode)).toList();
        this.errors = new ArrayList<>(domainModel.errors);
        return this;
    }
}
