package cz.muni.xmichalk.dto;

import java.util.Collection;

public class SearchResultsDTO {
    public Collection<FoundResultDTO> results;
    public Collection<String> errors;

    public SearchResultsDTO() {
    }

    public SearchResultsDTO(Collection<FoundResultDTO> results, Collection<String> errors) {
        this.results = results;
        this.errors = errors;
    }
}
