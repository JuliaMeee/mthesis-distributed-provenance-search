import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.queries.EQueryType;
import cz.muni.xmichalk.queries.IQueryEvaluator;
import cz.muni.xmichalk.queries.QueryEvaluatorsProvider;
import cz.muni.xmichalk.targetSpecification.ICondition;
import cz.muni.xmichalk.targetSpecification.bundleConditions.CountCondition;
import cz.muni.xmichalk.targetSpecification.bundleConditions.EComparisonResult;
import cz.muni.xmichalk.targetSpecification.findable.FindNodes;
import cz.muni.xmichalk.targetSpecification.nodeConditions.HasAttr;
import cz.muni.xmichalk.targetSpecification.nodeConditions.HasAttrQualifiedNameValue;
import cz.muni.xmichalk.targetSpecification.nodeConditions.HasId;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.NameSpaceConstants;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.muni.xmichalk.util.ProvDocumentUtils.deserialize;

public class QueriesTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    Map<EQueryType, IQueryEvaluator<?>> queryEvaluators = QueryEvaluatorsProvider.getQueryEvaluators();
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void findNodeIdsById() throws IOException {
        CpmDocument cpmDoc = TestDocument.getProcessingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.NODE_IDS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "NiceMarineStation", "blank");
        ICondition<INode> nodeSpecification = new HasId(nodeIdToFind.getUri());
        JsonNode querySpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof List<?>;
        List<?> foundNodeIds = (List<?>) result;
        assert foundNodeIds.size() == 1;
        QualifiedNameData qnData = (QualifiedNameData) foundNodeIds.getFirst();
        QualifiedName foundNodeId = qnData.toQN();
        assert foundNodeId.getUri().equals(nodeIdToFind.getUri());
        assert cpmDoc.getNode(nodeIdToFind) != null;
    }

    @Test
    public void findNodesById() throws IOException {
        CpmDocument cpmDoc = TestDocument.getProcessingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.NODES);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "NiceMarineStation", "blank");
        ICondition<INode> nodeSpecification = new HasId(nodeIdToFind.getUri());
        JsonNode querySpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof JsonNode;
        JsonNode foundNodesDoc = (JsonNode) result;
        Document resultsDoc = deserialize(foundNodesDoc.toString(), Formats.ProvFormat.JSON);
        assert resultsDoc != null;
        CpmDocument resultsCpmDoc = new CpmDocument(resultsDoc, pF, cPF, cF);
        INode foundNode = resultsCpmDoc.getNode(nodeIdToFind);
        assert foundNode != null;
        assert foundNode.getId().getUri().equals(nodeIdToFind.getUri());
    }

    @Test
    public void findNodeIdsByType() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.NODE_IDS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String attributeName = NameSpaceConstants.PROV_URI + "type";
        String attributeValue = NameSpaceConstants.SCHEMA_URI + "Person";
        ICondition<INode> nodeSpecification = new HasAttrQualifiedNameValue(
                attributeName,
                attributeValue
        );

        JsonNode querySpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof List<?>;
        List<?> list = (List<?>) result;
        assert list.size() == 2;
        List<QualifiedName> foundNodeIds = new ArrayList<>();
        for (Object obj : list) {
            QualifiedNameData qnData = (QualifiedNameData) obj;
            QualifiedName qn = qnData.toQN();
            foundNodeIds.add(qn);
        }
        assert foundNodeIds.stream().anyMatch(qn -> qn.getUri().equals("https://orcid.org/0000-0001-0001-0001"));
        assert foundNodeIds.stream().anyMatch(qn -> qn.getUri().equals("https://orcid.org/0000-0001-0001-0002"));
    }

    @Test
    public void findNodesByHasAttr() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.NODES);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String attributeName = NameSpaceConstants.SCHEMA_URI + "url";
        ICondition<INode> nodeSpecification = new HasAttr(attributeName);

        JsonNode querySpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof JsonNode;
        JsonNode foundNodesDoc = (JsonNode) result;
        Document resultsDoc = deserialize(foundNodesDoc.toString(), Formats.ProvFormat.JSON);
        assert resultsDoc != null;
        CpmDocument resultsCpmDoc = new CpmDocument(resultsDoc, pF, cPF, cF);
        assert resultsCpmDoc.getNodes().size() == 3;

        for (INode node : resultsCpmDoc.getNodes()) {
            assert CpmUtils.getAttributeValue(node, attributeName) != null;
        }
    }

    @Test
    public void testFits() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.TEST_FITS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        ICondition<INode> samplingNodeSpecification = new HasId(".*Sampling");
        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(
                        samplingNodeSpecification
                ),
                EComparisonResult.EQUALS,
                1
        );
        JsonNode querySpecification = objectMapper.valueToTree(bundleSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);
        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert fits;
    }

    @Test
    public void testDoesNotFit() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.TEST_FITS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        ICondition<INode> backwardConnectorSpecification = new HasAttrQualifiedNameValue(
                NameSpaceConstants.PROV_URI + "type",
                NameSpaceConstants.CPM_URI + "backwardConnector"
        );
        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(
                        backwardConnectorSpecification
                ),
                EComparisonResult.GREATER_THAN_OR_EQUALS,
                1
        );

        JsonNode querySpecification = objectMapper.valueToTree(bundleSpecification);

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);
        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert !fits;
    }

    @Test
    public void findBackwardConnectors() throws IOException {
        CpmDocument cpmDoc = TestDocument.getProcessingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.CONNECTORS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        JsonNode querySpecification = objectMapper.valueToTree("backward");

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof List;
        List<?> list = (List<?>) result;
        assert list.size() == 1;
        assert list.getFirst() instanceof ConnectorData;
        ConnectorData connectorData = (ConnectorData) list.getFirst();
        assert connectorData.id.toQN().getUri().equals(NameSpaceConstants.BLANK_URI + "StoredSampleCon_r1");
    }

    @Test
    public void findForwardConnectors() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        IQueryEvaluator<?> queryEvaluator = queryEvaluators.get(EQueryType.CONNECTORS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        JsonNode querySpecification = objectMapper.valueToTree("forward");

        Object result = queryEvaluator.apply(cpmDoc, startNodeId, querySpecification);

        assert result != null;
        assert result instanceof List;
        List<?> list = (List<?>) result;
        assert list.size() == 6;
    }
}
