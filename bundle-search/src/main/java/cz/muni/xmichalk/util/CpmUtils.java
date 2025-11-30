package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.StatementOrBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_REFERENCED_META_BUNDLE_ID;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;

public class CpmUtils {
    public static QualifiedName getMetaBundleId(CpmDocument bundle) {
        INode startNode = chooseStartNode(bundle);
        if (startNode == null) {
            return null;
        }
        List<INode> mainActivities = BundleTraverser.traverseAndFindNodes(
                startNode,
                node -> AttributeUtils.hasAttributeTargetValue(node, ATTR_PROV_TYPE, QualifiedName.class,
                        qName -> qName.getUri().equals(CPM_URI + "mainActivity")),
                null
        );

        if (mainActivities == null || mainActivities.size() != 1) {
            return null;
        }

        return (QualifiedName) AttributeUtils.getAttributeValue(mainActivities.getFirst(), ATTR_REFERENCED_META_BUNDLE_ID);
    }

    public static QualifiedName getConnectorIdInReferencedBundle(INode connectorNode) {
        // backward connector id matches general forward connector id in the other bundle
        // specific forward connector id is not present in the other bundle as a backward connector, resolve it to general forward connector id
        Object provType = AttributeUtils.getAttributeValue(connectorNode, ATTR_PROV_TYPE);
        if (provType == null) {
            return null;
        }
        if (AttributeUtils.isTargetValue(provType, QualifiedName.class, qn -> qn.getUri().equals(CPM_URI + "backwardConnector"))) {
            return connectorNode.getId();
        }
        if (AttributeUtils.isTargetValue(provType, QualifiedName.class, qn -> qn.getUri().equals(CPM_URI + "forwardConnector"))) {
            ArrayList<Predicate<EdgeToNode>> subgraphConstraints = new ArrayList<Predicate<EdgeToNode>>();
            subgraphConstraints.add(edgeToNode -> edgeToNode.node.equals(connectorNode));
            subgraphConstraints.add(edgeToNode -> {
                boolean isSpecialization = edgeToNode.edge.getRelations().stream().anyMatch((relation) -> relation.getKind() == StatementOrBundle.Kind.PROV_SPECIALIZATION);
                boolean isGeneralEntity = edgeToNode.edge.getCause() == edgeToNode.node;
                boolean isForwardConnector = AttributeUtils.hasAttributeTargetValue(edgeToNode.node, ATTR_PROV_TYPE, QualifiedName.class,
                        qn -> qn.getUri().equals(CPM_URI + "forwardConnector"));
                return isSpecialization && isGeneralEntity && isForwardConnector;
            });
            List<List<EdgeToNode>> subgraphs = LinearSubgraphFinder.findSubgraphsFrom(connectorNode, subgraphConstraints);
            if (subgraphs.isEmpty() || subgraphs.getFirst().size() != 2) {
                return null;
            }
            return subgraphs.getFirst().getLast().node.getId();
        }

        return null;
    }

    public static INode chooseStartNode(CpmDocument document) {
        List<INode> forwardConnectors = document.getForwardConnectors();
        if (!forwardConnectors.isEmpty()) {
            return forwardConnectors.getFirst();
        }
        List<INode> backwardConnectors = document.getBackwardConnectors();
        if (!backwardConnectors.isEmpty()) {
            return backwardConnectors.getFirst();
        }
        INode mainActivity = document.getMainActivity();
        if (mainActivity != null) {
            return mainActivity;
        }
        List<INode> nodes = document.getNodes();
        if (!nodes.isEmpty()) {
            return nodes.getFirst();
        }
        return null;
    }

}
