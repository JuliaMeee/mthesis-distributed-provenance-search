package cz.muni.xmichalk.validity;

import cz.muni.xmichalk.dto.BundleSearchResultDTO;
import cz.muni.xmichalk.models.ItemToSearch;

public interface IValidityVerifier {
    boolean verify(ItemToSearch itemToSearch, BundleSearchResultDTO bundleSearchResult);
}
