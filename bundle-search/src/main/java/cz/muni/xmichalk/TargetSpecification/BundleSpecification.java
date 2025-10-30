package cz.muni.xmichalk.TargetSpecification;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.model.QualifiedName;

import java.util.ArrayList;
import java.util.List;

public class BundleSpecification {
    public List<CountSpecification> specifications;

    public BundleSpecification() {
    }

    public BundleSpecification(List<CountSpecification> specifications) {
        this.specifications = new ArrayList<CountSpecification>(specifications);
    }

    public boolean test(CpmDocument document, QualifiedName startNodeId) {

        if (specifications == null || specifications.isEmpty()) {
            return true;
        }

        INode startNode = document.getNode(startNodeId);
        if (startNode == null) {
            throw new IllegalArgumentException("Start node with id " + startNodeId.getUri() + " does not exist in document " + document.getBundleId().getUri());
        }

        for (CountSpecification spec : specifications) {
            if (spec == null) {
                continue;
            }
            if (!spec.test(startNode)) {
                return false;
            }
        }

        return true;
    }

}
