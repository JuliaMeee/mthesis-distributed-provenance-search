package cz.muni.xmichalk.SearchPriority;

import cz.muni.xmichalk.Models.ItemToSearch;
import cz.muni.xmichalk.Models.SearchParams;

import java.util.Comparator;

public class SearchPriorityComparatorResolver {
    private final static ESearchPriority defaultPriority = ESearchPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS;

    public static Comparator<ItemToSearch> getSearchPriorityComparator(SearchParams params) {
        ESearchPriority searchPriority = params.searchPriority != null ? params.searchPriority : defaultPriority;

        switch (searchPriority) {
            case ESearchPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS:
                return new IntegrityThenOrderedValidity(params.validityChecks);
            // Extend with more options in the future
            default:
                throw new IllegalArgumentException("Unknown search priority: " + searchPriority);
        }
    }
}
