package cz.muni.xmichalk.dto;

import java.util.Collection;

public class TraversalResultsDTO {
    public Collection<FoundResultDTO> results;
    public Collection<String> errors;

    public TraversalResultsDTO() {
    }

    public TraversalResultsDTO(Collection<FoundResultDTO> results, Collection<String> errors) {
        this.results = results;
        this.errors = errors;
    }
}
