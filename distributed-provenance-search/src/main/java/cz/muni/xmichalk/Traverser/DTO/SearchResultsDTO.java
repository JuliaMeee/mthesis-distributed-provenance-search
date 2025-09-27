package cz.muni.xmichalk.Traverser.DTO;

import cz.muni.xmichalk.Traverser.Models.SearchResults;

import java.util.List;

public class SearchResultsDTO implements IDTO<SearchResults> {
    public List<InnerNodeDTO> results;

    @Override
    public SearchResults toDomainModel() {
        return new SearchResults(
                this.results.stream().map(InnerNodeDTO::toDomainModel).toList()
        );
    }

    @Override
    public SearchResultsDTO from(final SearchResults domainModel) {
        this.results = domainModel.results.stream().map(innerNode -> new InnerNodeDTO().from(innerNode)).toList();
        return this;
    }
}
