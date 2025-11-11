package cz.muni.xmichalk.DocumentValidity.ValidityVerifier;

import cz.muni.xmichalk.DTO.BundleSearchResultDTO;
import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.DocumentValidity.UnsuportedValiditySpecificationException;
import cz.muni.xmichalk.Models.ItemToSearch;
import org.apache.commons.collections4.map.HashedMap;

import java.util.List;
import java.util.Map;

public class ValidityVerifierRegistry {
    private final Map<EValiditySpecification, IValidityVerifier> registry = new HashedMap<>();

    public ValidityVerifierRegistry(Map<EValiditySpecification, IValidityVerifier> registry) {
        this.registry.putAll(registry);
    }

    public IValidityVerifier getValidityVerifier(EValiditySpecification validity) {
        return registry.get(validity);
    }

    public boolean verifyValidity(ItemToSearch itemToSearch, BundleSearchResultDTO bundleSearchResult, EValiditySpecification validitySpecification) throws UnsuportedValiditySpecificationException {
        IValidityVerifier verifier = registry.get(validitySpecification);
        if (verifier == null) {
            throw new UnsuportedValiditySpecificationException("No validity verifier registered for validity specification: " + validitySpecification);
        }

        return verifier.verifyValidity(itemToSearch, bundleSearchResult);
    }

    public List<EValiditySpecification> getAllValiditySpecifications() {

        return registry.keySet().stream().toList();
    }
}
