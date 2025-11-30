package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.EdgeToNode;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.querySpecification.findable.FindFittingNodes;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;
import cz.muni.xmichalk.querySpecification.nodeConditions.HasAttrQualifiedNameValue;
import cz.muni.xmichalk.util.AttributeUtils;
import cz.muni.xmichalk.util.CpmUtils;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import java.util.List;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;

public class GetConnectors implements IQuery<List<ConnectorData>> {
    public Boolean backward;
    public ICondition<EdgeToNode> pathCondition;

    public GetConnectors() {
    }

    public GetConnectors(Boolean backward) {
        this.backward = backward;
    }

    public GetConnectors(Boolean backward, ICondition<EdgeToNode> pathCondition) {
        this.backward = backward;
        this.pathCondition = pathCondition;
    }


    @Override
    public List<ConnectorData> evaluate(BundleStart input) {
        if (backward == null) return null;

        IFindableInDocument<INode> finder = new FindFittingNodes(
                new HasAttrQualifiedNameValue(
                        ATTR_PROV_TYPE.getUri(),
                        CPM_URI + ((backward) ? "backwardConnector" : "forwardConnector")
                ),
                pathCondition
        );

        List<INode> foundNodes = finder.find(input.bundle, input.startNode);

        if (foundNodes == null || foundNodes.isEmpty()) return null;

        return foundNodes.stream()
                .map(this::transformToConnectorData)
                .toList();
    }

    private ConnectorData transformToConnectorData(INode node) {
        ConnectorData connectorData = new ConnectorData();


        connectorData.id = new QualifiedNameData().from(node.getId());

        QualifiedName referencedConnectorIdValue = CpmUtils.getConnectorIdInReferencedBundle(node);
        connectorData.referencedConnectorId =
                new QualifiedNameData().from(referencedConnectorIdValue == null ? node.getId() : referencedConnectorIdValue);

        Object referencedBundleIdValue = AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_ID);
        connectorData.referencedBundleId = referencedBundleIdValue == null ? null :
                new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedBundleIdValue);

        Object referencedMetaBundleIdValue = AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_META_BUNDLE_ID);
        connectorData.referencedMetaBundleId = referencedMetaBundleIdValue == null ? null :
                new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedMetaBundleIdValue);

        Object provServiceUriValue = AttributeUtils.getAttributeValue(node, ATTR_PROVENANCE_SERVICE_URI);
        connectorData.provenanceServiceUri = provServiceUriValue == null ? null : ((LangString) provServiceUriValue).getValue();


        LangString bundleHashValueObj = (LangString) AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_HASH_VALUE);
        connectorData.referencedBundleHashValue = bundleHashValueObj != null ? bundleHashValueObj.getValue() : null;

        LangString hashAlgObj = (LangString) AttributeUtils.getAttributeValue(node, ATTR_HASH_ALG);
        connectorData.hashAlg = hashAlgObj != null ? hashAlgObj.getValue() : null;

        return connectorData;
    }
}
