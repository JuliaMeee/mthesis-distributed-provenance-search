import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.queries.*;
import cz.muni.xmichalk.querySpecification.bundleConditions.CountCondition;
import cz.muni.xmichalk.querySpecification.bundleConditions.EComparisonResult;
import cz.muni.xmichalk.querySpecification.findable.FindFittingLinearSubgraphs;
import cz.muni.xmichalk.querySpecification.findable.FindFittingNodes;
import cz.muni.xmichalk.querySpecification.logicalOperations.AllTrue;
import cz.muni.xmichalk.querySpecification.nodeConditions.*;
import cz.muni.xmichalk.querySpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.querySpecification.subgraphConditions.edgeConditions.IsRelation;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.NameSpaceConstants;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.util.NameSpaceConstants.BLANK_URI;
import static cz.muni.xmichalk.util.NameSpaceConstants.SCHEMA_URI;
import static cz.muni.xmichalk.util.ProvDocumentUtils.deserialize;

public class QueriesTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void findNodeIdsById() throws IOException {
        CpmDocument cpmDoc = TestDocument.getProcessingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "NiceMarineStation", "blank");
        IQuery<List<QualifiedNameData>> query = new GetNodeIds(
                new FindFittingNodes(
                        new HasId(nodeIdToFind.getUri())
                )
        );
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

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

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "NiceMarineStation", "blank");
        IQuery<?> query = new GetNodes(
                new FindFittingNodes(
                        new HasId(nodeIdToFind.getUri())
                )
        );
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

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

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        IQuery<?> query = new GetNodeIds(
                new FindFittingNodes(
                        new HasAttrQualifiedNameValue(
                                NameSpaceConstants.PROV_URI + "type",
                                SCHEMA_URI + "Person"
                        )
                )
        );
        JsonNode serializedQuery = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedQuery, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

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

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String attributeName = SCHEMA_URI + "url";
        IQuery<?> query = new GetNodes(new FindFittingNodes(
                new HasAttr(attributeName)
        ));
        JsonNode serializedQuery = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedQuery, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

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
    public void testFindSubgraph() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        IQuery<?> query = new GetSubgraphs(new FindFittingLinearSubgraphs(List.of(
                new EdgeToNodeCondition(
                        null,
                        new AllTrue<>(List.of(
                                new HasAttrQualifiedNameValue(
                                        ATTR_PROV_TYPE.getUri(),
                                        SCHEMA_URI + "Person"),
                                new HasAttrLangStringValue(
                                        SCHEMA_URI + "name",
                                        null,
                                        "Jane Smith"
                                )
                        )),
                        null
                ),
                new EdgeToNodeCondition(
                        new IsRelation(StatementOrBundle.Kind.PROV_ASSOCIATION),
                        new IsKind(StatementOrBundle.Kind.PROV_ACTIVITY),
                        null
                )
        )));
        JsonNode serializedQuery = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedQuery, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

        // Results assertions

        assert result != null;
        assert result instanceof Collection<?>;
        Collection<?> collection = (Collection<?>) result;
        assert (collection.size() == 2);
        List<CpmDocument> subgraphDocs = new ArrayList<>();
        for (Object subgraphObject : collection) {
            assert subgraphObject instanceof JsonNode;
            JsonNode subgraphJsonNode = (JsonNode) subgraphObject;
            Document subgraphDoc = deserialize(subgraphJsonNode.toString(), Formats.ProvFormat.JSON);
            assert subgraphDoc != null;
            CpmDocument subgraphCpmDoc = new CpmDocument(subgraphDoc, pF, cPF, cF);
            subgraphDocs.add(subgraphCpmDoc);
        }

        QualifiedName JaneSmithId = new org.openprovenance.prov.vanilla.QualifiedName("https://orcid.org/", "0000-0001-0001-0001", "orcid");
        QualifiedName activity1Id = new org.openprovenance.prov.vanilla.QualifiedName("gen/", "36294ecf35d15ba81da0bb55dfd8ee07934568e28ec56a51774b3e331ed6fd99", "gen");
        QualifiedName activity2Id = new org.openprovenance.prov.vanilla.QualifiedName("gen/", "190fd43b1968737f3501420a6bfd9b74873e32416c6e14fef26238fbe3b197a2", "gen");


        assert subgraphDocs.stream().allMatch(subgraphDoc -> subgraphDoc.getNodes().size() == 2);
        assert subgraphDocs.stream().allMatch(subgraphDoc -> subgraphDoc.getNode(JaneSmithId) != null);
        assert subgraphDocs.stream().anyMatch(subgraphDoc -> subgraphDoc.getNode(activity1Id) != null);
        assert subgraphDocs.stream().anyMatch(subgraphDoc -> subgraphDoc.getNode(activity2Id) != null);

    }

    @Test
    public void testFits() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        IQuery<?> query = new TestBundleFits(
                new CountCondition(
                        new FindFittingNodes(
                                new HasId(".*Sampling")
                        ),
                        EComparisonResult.EQUALS,
                        1
                ));
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));
        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert fits;
    }

    @Test
    public void testDoesNotFit() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        IQuery<?> query = new TestBundleFits(
                new CountCondition(
                        new FindFittingNodes(
                                new HasAttrQualifiedNameValue(
                                        NameSpaceConstants.PROV_URI + "type",
                                        NameSpaceConstants.CPM_URI + "backwardConnector"
                                )
                        ),
                        EComparisonResult.GREATER_THAN_OR_EQUALS,
                        1
                )
        );
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert !fits;
    }

    @Test
    public void findBackwardConnectors() throws IOException {
        CpmDocument cpmDoc = TestDocument.getProcessingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "ProcessedSampleConSpec", "blank");
        IQuery<?> query = new GetConnectors(true);
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

        assert result != null;
        assert result instanceof List;
        List<?> list = (List<?>) result;
        assert list.size() == 1;
        assert list.getFirst() instanceof ConnectorData;
        ConnectorData connectorData = (ConnectorData) list.getFirst();
        assert connectorData.id.toQN().getUri().equals(BLANK_URI + "StoredSampleCon_r1");
    }

    @Test
    public void findForwardConnectors() throws IOException {
        CpmDocument cpmDoc = TestDocument.getSamplingBundle_V1();

        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        IQuery<?> query = new GetConnectors(false);
        JsonNode serializedSpecification = objectMapper.valueToTree(query);
        IQuery<?> deserializedQuery = objectMapper.convertValue(serializedSpecification, new TypeReference<IQuery<?>>() {
        });

        Object result = deserializedQuery.evaluate(new BundleStart(cpmDoc, cpmDoc.getNode(startNodeId)));

        assert result != null;
        assert result instanceof List;
        List<?> list = (List<?>) result;
        assert list.size() == 6;
    }
}
