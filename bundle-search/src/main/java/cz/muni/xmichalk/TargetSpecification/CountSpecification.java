package cz.muni.xmichalk.TargetSpecification;

import cz.muni.fi.cpm.model.INode;

public class CountSpecification {
    public ICountableInDocument countableInDocument;
    public EComparisonResult comparisonResult;
    public Integer count;

    public CountSpecification() {
    }

    public CountSpecification(ICountableInDocument countableInDocument, EComparisonResult comparisonResult, Integer count) {
        this.countableInDocument = countableInDocument;
        this.comparisonResult = comparisonResult;
        this.count = count;
    }

    public boolean test(INode startNode) {
        if (count == null || comparisonResult == null || countableInDocument == null) {
            throw new IllegalStateException("Missing values in count specification.");
        }

        int actualCount = countableInDocument.countInDocument(startNode);

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
