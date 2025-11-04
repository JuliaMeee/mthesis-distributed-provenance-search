package cz.muni.xmichalk.TargetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;

import java.util.ArrayList;
import java.util.List;

public class BundleSpecification implements ITestableSpecification<CpmDocument> {
    public List<ITestableSpecification<CpmDocument>> specifications;

    public BundleSpecification() {
    }

    public BundleSpecification(List<ITestableSpecification<CpmDocument>> specifications) {
        this.specifications = new ArrayList<ITestableSpecification<CpmDocument>>(specifications);
    }

    public boolean test(CpmDocument document) {

        if (specifications == null || specifications.isEmpty()) {
            return true;
        }

        for (ITestableSpecification<CpmDocument> spec : specifications) {
            if (spec == null) {
                continue;
            }
            if (!spec.test(document)) {
                return false;
            }
        }

        return true;
    }

}
