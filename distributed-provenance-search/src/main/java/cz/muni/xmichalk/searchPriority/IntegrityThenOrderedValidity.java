package cz.muni.xmichalk.searchPriority;

import cz.muni.xmichalk.models.ItemToSearch;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.Comparator;

public class IntegrityThenOrderedValidity implements Comparator<ItemToSearch> {
    public IntegrityThenOrderedValidity() {
    }

    @Override
    public int compare(ItemToSearch first, ItemToSearch second) {
        if (first.pathIntegrity && !second.pathIntegrity) {
            return -1;
        }
        if (!first.pathIntegrity && second.pathIntegrity) {
            return 1;
        }
        if (first.pathValidityChecks.size() != second.pathValidityChecks.size()) {
            return Integer.compare(second.pathValidityChecks.size(), first.pathValidityChecks.size());
        }

        for (EValidityCheck validityCheck : first.pathValidityChecks.keySet()) {
            boolean firstHasCheck = first.pathValidityChecks.getOrDefault(validityCheck, false);
            boolean secondHasCheck = second.pathValidityChecks.getOrDefault(validityCheck, false);
            if (firstHasCheck && !secondHasCheck) {
                return -1;
            }
            if (!firstHasCheck && secondHasCheck) {
                return 1;
            }
        }

        return 0;
    }
}
