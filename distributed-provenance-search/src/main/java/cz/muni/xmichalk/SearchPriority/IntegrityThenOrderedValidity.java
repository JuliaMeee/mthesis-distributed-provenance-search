package cz.muni.xmichalk.SearchPriority;

import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.Models.ItemToSearch;

import java.util.Comparator;
import java.util.List;

public class IntegrityThenOrderedValidity implements Comparator<ItemToSearch> {
    private final List<EValiditySpecification> validityChecksOrder;

    public IntegrityThenOrderedValidity(List<EValiditySpecification> validityChecksOrder) {
        this.validityChecksOrder = validityChecksOrder;
    }

    @Override
    public int compare(ItemToSearch first, ItemToSearch second) {
        if (first.pathIntegrity && !second.pathIntegrity) {
            return -1;
        }
        if (!first.pathIntegrity && second.pathIntegrity) {
            return 1;
        }

        for (EValiditySpecification validityCheck : validityChecksOrder) {
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
