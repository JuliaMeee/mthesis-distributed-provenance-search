package cz.muni.xmichalk.DTO;

import cz.muni.xmichalk.Models.SearchResults;

import java.util.List;

public class SearchResultsDTO implements IDTO<SearchResults> {
    public List<FoundResultDTO> results;

    @Override
    public SearchResults toDomainModel() {
        return new SearchResults(
                this.results.stream().map(FoundResultDTO::toDomainModel).toList()
        );
    }

    @Override
    public SearchResultsDTO from(final SearchResults domainModel) {
        if (domainModel == null) {
            return null;
        }
        this.results = domainModel.results.stream().map(innerNode -> new FoundResultDTO().from(innerNode)).toList();
        return this;
    }
}
