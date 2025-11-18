package cz.muni.xmichalk.validity;

import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.models.ItemToTraverse;

public interface IValidityVerifier {
    boolean verify(ItemToTraverse itemToTraverse, BundleQueryResultDTO bundleSearchResult);
}
