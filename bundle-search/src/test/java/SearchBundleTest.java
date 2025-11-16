import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.bundleSearch.BundleSearcherProvider;
import cz.muni.xmichalk.bundleSearch.ETargetType;
import cz.muni.xmichalk.bundleSearch.ISearchBundle;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.targetSpecification.*;
import cz.muni.xmichalk.targetSpecification.attributeSpecification.QualifiedNameAttrSpecification;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.NameSpaceConstants;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.muni.xmichalk.util.ProvDocumentUtils.deserialize;
import static cz.muni.xmichalk.util.ProvDocumentUtils.deserializeFile;

public class SearchBundleTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";
    Map<ETargetType, ISearchBundle<?>> bundleSearchers = BundleSearcherProvider.getBundleSearchers();
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void findNodeIdsById() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.NODE_IDS_BY_ID);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "NiceMarineStation", "blank");
        JsonNode targetSpecification = objectMapper.valueToTree(nodeIdToFind.getUri());

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

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
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.NODES_BY_ID);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        QualifiedName nodeIdToFind = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "NiceMarineStation", "blank");
        JsonNode targetSpecification = objectMapper.valueToTree(nodeIdToFind.getUri());

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

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
    public void findNodeIdsBySpecification_Type() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.NODE_IDS_BY_SPECIFICATION);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String attributeName = NameSpaceConstants.PROV_URI + "type";
        String attributeValue = NameSpaceConstants.SCHEMA_URI + "Person";
        NodeSpecification nodeSpecification = new NodeSpecification();
        nodeSpecification.hasAttributeValues = List.of(
                new QualifiedNameAttrSpecification(
                        attributeName,
                        attributeValue
                )
        );

        JsonNode targetSpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

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
    public void findNodesBySpecification_HasAttr() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.NODES_BY_SPECIFICATION);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String attributeName = NameSpaceConstants.SCHEMA_URI + "url";
        NodeSpecification nodeSpecification = new NodeSpecification();
        nodeSpecification.hasAttributes = List.of(
                attributeName
        );

        JsonNode targetSpecification = objectMapper.valueToTree(nodeSpecification);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

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
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.TEST_FITS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        NodeSpecification samplingNodeSpecification = new NodeSpecification();
        samplingNodeSpecification.idUriRegex = ".*Sampling";
        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                new CountNodes(
                                        samplingNodeSpecification
                                ),
                                EComparisonResult.EQUALS,
                                1
                        )
                )
        );
        JsonNode targetSpecification = objectMapper.valueToTree(bundleSpecification);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);
        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert fits;
    }

    @Test
    public void testDoesNotFit() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.TEST_FITS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        NodeSpecification backwardConnectorSpecification = new NodeSpecification();
        backwardConnectorSpecification.hasAttributeValues = List.of(
                new QualifiedNameAttrSpecification(
                        NameSpaceConstants.PROV_URI + "type",
                        NameSpaceConstants.CPM_URI + "backwardConnector"
                )
        );
        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                new CountNodes(
                                        backwardConnectorSpecification
                                ),
                                EComparisonResult.GREATER_THAN_OR_EQUALS,
                                1
                        )
                )
        );

        JsonNode targetSpecification = objectMapper.valueToTree(bundleSpecification);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);
        assert result != null;
        assert result instanceof Boolean;
        Boolean fits = (Boolean) result;
        assert !fits;
    }

    @Test
    public void findBackwardConnectors() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.CONNECTORS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "ProcessedSampleConSpec", "blank");
        JsonNode targetSpecification = objectMapper.valueToTree("backward");

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

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
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.CONNECTORS);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        JsonNode targetSpecification = objectMapper.valueToTree("forward");

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);

        assert result != null;
        assert result instanceof List;
        List<?> list = (List<?>) result;
        assert list.size() == 6;
    }

    @Test
    public void findBundleIdByMetaBundleId() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.BUNDLE_ID_BY_META_BUNDLE_ID);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1_Spec", "blank");
        String metaBundleUri = "http://prov-storage-1:8000/api/v1/documents/meta/SamplingBundle_V0_meta";
        JsonNode targetSpecification = objectMapper.valueToTree(metaBundleUri);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);
        assert result != null;
        assert result instanceof QualifiedNameData;
        QualifiedNameData bundleIdData = (QualifiedNameData) result;
        assert bundleIdData.toQN().getUri().equals(cpmDoc.getBundleId().getUri());
    }

    @Test
    public void failToFindBundleIdByMetaBundleId() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ISearchBundle<?> bundleSearcher = bundleSearchers.get(ETargetType.BUNDLE_ID_BY_META_BUNDLE_ID);
        QualifiedName startNodeId = new org.openprovenance.prov.vanilla.QualifiedName(NameSpaceConstants.BLANK_URI, "StoredSampleCon_r1", "blank");
        String metaBundleUri = "http://prov-storage-1:8000/api/v1/documents/meta/SamplingBundle_V0_meta";
        JsonNode targetSpecification = objectMapper.valueToTree(metaBundleUri);

        Object result = bundleSearcher.apply(cpmDoc, startNodeId, targetSpecification);
        assert result == null;
    }
}
