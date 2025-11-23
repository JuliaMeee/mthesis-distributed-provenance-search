package cz.muni.xmichalk.querySpecification.bundleConditions;

import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;

public class CountCondition implements ICondition<BundleStart> {
    public IFindableInDocument<?> findableInDocument;
    public EComparisonResult comparisonResult;
    public Integer count;

    public CountCondition() {
    }

    public CountCondition(IFindableInDocument countableInDocument, EComparisonResult comparisonResult, Integer count) {
        this.findableInDocument = countableInDocument;
        this.comparisonResult = comparisonResult;
        this.count = count;
    }

    public boolean test(BundleStart target) {
        if (count == null || comparisonResult == null || findableInDocument == null) {
            throw new IllegalStateException("Missing values in count specification.");
        }

        int actualCount = findableInDocument.find(target.bundle, target.startNode).size();

        return switch (comparisonResult) {
            case EComparisonResult.EQUALS -> actualCount == count;
            case EComparisonResult.LESS_THAN -> actualCount < count;
            case EComparisonResult.LESS_THAN_OR_EQUALS -> actualCount <= count;
            case EComparisonResult.GREATER_THAN -> actualCount > count;
            case EComparisonResult.GREATER_THAN_OR_EQUALS -> actualCount >= count;
            default -> throw new IllegalStateException("Unexpected comparison result: " + comparisonResult);
        };
    }

}
