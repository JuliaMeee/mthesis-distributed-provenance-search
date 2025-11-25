package cz.muni.xmichalk.traversalPriority;

import cz.muni.xmichalk.models.ItemToTraverse;
import cz.muni.xmichalk.validity.EValidityCheck;

import java.util.Comparator;

public class IntegrityThenOrderedValidity implements Comparator<ItemToTraverse> {
    public IntegrityThenOrderedValidity() {
    }

    @Override
    public int compare(ItemToTraverse first, ItemToTraverse second) {
        if (first == null) return 1;
        if (second == null) return -1;
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
