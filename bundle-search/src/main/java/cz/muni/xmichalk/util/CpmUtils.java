package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.EdgeToNode;
import org.openprovenance.prov.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;

public class CpmUtils {
    public static QualifiedName getMetaBundleId(CpmDocument bundle) {
        INode startNode = chooseStartNode(bundle);
        if (startNode == null) {
            return null;
        }
        List<INode> mainActivities = BundleNodesTraverser.traverseAndFind(bundle, startNode.getId(),
                node -> hasAttributeTargetValue(node, ATTR_PROV_TYPE, QualifiedName.class,
                        qName -> qName.getUri().equals(CPM_URI + "mainActivity"))
        );

        if (mainActivities == null || mainActivities.size() != 1) {
            return null;
        }

        return (QualifiedName) getAttributeValue(mainActivities.getFirst(), ATTR_REFERENCED_META_BUNDLE_ID);
    }

    public static QualifiedName getConnectorIdInReferencedBundle(INode connectorNode) {
        // backward connector id matches general forward connector id in the other bundle
        // specific forward connector id is not present in the other bundle as a backward connector, resolve it to general forward connector id
        Object provType = getAttributeValue(connectorNode, ATTR_PROV_TYPE);
        if (provType == null) {
            return null;
        }
        if (isTargetValue(provType, QualifiedName.class, qn -> qn.getUri().equals(CPM_URI + "backwardConnector"))) {
            return connectorNode.getId();
        }
        if (isTargetValue(provType, QualifiedName.class, qn -> qn.getUri().equals(CPM_URI + "forwardConnector"))) {
            ArrayList<Predicate<EdgeToNode>> subgraphConstraints = new ArrayList<Predicate<EdgeToNode>>();
            subgraphConstraints.add(edgeToNode -> edgeToNode.node.equals(connectorNode));
            subgraphConstraints.add(edgeToNode -> {
                boolean isSpecialization = edgeToNode.edge.getRelations().stream().anyMatch((relation) -> relation.getKind() == StatementOrBundle.Kind.PROV_SPECIALIZATION);
                boolean isGeneralEntity = edgeToNode.edge.getCause() == edgeToNode.node;
                boolean isForwardConnector = hasAttributeTargetValue(edgeToNode.node, ATTR_PROV_TYPE, QualifiedName.class,
                        qn -> qn.getUri().equals(CPM_URI + "forwardConnector"));
                return isSpecialization && isGeneralEntity && isForwardConnector;
            });
            List<List<EdgeToNode>> subgraphs = LinearSubgraphFinder.findFrom(connectorNode, subgraphConstraints);
            if (subgraphs.isEmpty() || subgraphs.getFirst().size() != 2) {
                return null;
            }
            return subgraphs.getFirst().getLast().node.getId();
        }

        return null;
    }

    public static <T> boolean hasAttributeTargetValue(INode node, QualifiedName attributeName, Class<T> targetClass, Predicate<T> isTargetValue) {
        Object value = getAttributeValue(node, attributeName);
        return isTargetValue(value, targetClass, isTargetValue);
    }

    public static <T> boolean hasAttributeTargetValue(INode node, String attributeNameUri, Class<T> targetClass, Predicate<T> isTargetValue) {
        Object value = getAttributeValue(node, attributeNameUri);
        return isTargetValue(value, targetClass, isTargetValue);
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

    public static Object getAttributeValue(INode node, QualifiedName attributeName) {
        return getAttributeValue(node, attributeName.getUri());
    }

    public static Object getAttributeValue(INode node, String attributeNameUri) {
        List<Object> values = new ArrayList<>();
        boolean isList = false;
        boolean found = false;

        for (Element element : node.getElements()) {
            Object value = getAttributeValue(element, attributeNameUri);
            if (value == null) {
                continue;
            }

            found = true;
            if (value instanceof Collection) {
                isList = true;
                values.addAll((Collection<?>) value);
            } else {
                values.add(value);
            }
        }

        if (!found) return null; // does not have the attribute
        if (isList) return values; // found list(s) of values (possibly empty)
        if (values.size() == 1) return values.getFirst(); // only found one value instance and not a list
        return values; // multiple single values found
    }

    public static Object getAttributeValue(Element element, QualifiedName attributeName) {
        return getAttributeValue(element, attributeName.getUri());
    }

    public static Object getAttributeValue(Element element, String attributeNameUri) {
        if (element.getKind() == StatementOrBundle.Kind.PROV_ACTIVITY) {
            if (attributeNameUri.equals(ATTR_START_TIME.getUri())) {
                return ((Activity) element).getStartTime();
            } else if (attributeNameUri.equals(ATTR_END_TIME.getUri())) {
                return ((Activity) element).getEndTime();
            }
        }
        if (attributeNameUri.equals(ATTR_LOCATION.getUri())) {
            return element.getLocation();
        }
        if (attributeNameUri.equals(ATTR_PROV_TYPE.getUri())) {
            for (Type type : element.getType()) {
                if (type.getElementName().getUri().equals(attributeNameUri)) {
                    return type.getValue();
                }
            }
        }
        if (attributeNameUri.equals(ATTR_LABEL.getUri())) {
            if (element.getLabel() != null) {
                return element.getLabel();
            }
        }
        for (Other other : element.getOther()) {
            org.openprovenance.prov.model.QualifiedName name = other.getElementName();
            if (name.getUri().equals(attributeNameUri)) {
                return other.getValue();
            }
        }

        return null;
    }

    public static <T> boolean isTargetValue(Object value, Class<T> targetClass, Predicate<T> predicate) {

        if (targetClass.isInstance(value)) {
            return predicate.test(targetClass.cast(value));
        }

        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (targetClass.isInstance(item)) {
                    if (predicate.test(targetClass.cast(item))) {
                        return true;
                    }
                }
            }
            return false;
        }

        return false;
    }
}
