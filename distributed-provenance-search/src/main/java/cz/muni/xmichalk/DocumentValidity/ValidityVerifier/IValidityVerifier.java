package cz.muni.xmichalk.DocumentValidity.ValidityVerifier;

import cz.muni.xmichalk.DTO.BundleSearchResultDTO;
import cz.muni.xmichalk.Models.ItemToSearch;

public interface IValidityVerifier {
    boolean verifyValidity(ItemToSearch itemToSearch, BundleSearchResultDTO bundleSearchResult);
}
