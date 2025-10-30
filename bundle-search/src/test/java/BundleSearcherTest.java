import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.TargetSpecification.AttributeSpecification.QualifiedNameAttrSpecification;
import cz.muni.xmichalk.TargetSpecification.*;
import cz.muni.xmichalk.Util.CpmUtils;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static cz.muni.xmichalk.Util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.Util.NameSpaceConstants.BLANK_URI;
import static cz.muni.xmichalk.Util.NameSpaceConstants.CPM_URI;
import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserializeFile;

public class BundleSearcherTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ProvUtilities u = new ProvUtilities();
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";

    @Test
    public void testLoadPartialNode() {
        var file = Path.of(dataFolder + "nodeSpecs.json");
        try {
            var doc = deserializeFile(file, Formats.ProvFormat.JSON);
            var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
            assert cpmDoc.getNodes().size() == 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPickNewestVersion() throws IOException {
        var file = Path.of(dataFolder + "metaDocument.json");

        var document = deserializeFile(file, Formats.ProvFormat.JSON);

        var cpmDoc = new CpmDocument(document, pF, cPF, cF);

        var bundleId = LatestVersionPicker.pickFrom(
                new org.openprovenance.prov.vanilla.QualifiedName("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/", "SamplingBundle_V0", "storage"),
                cpmDoc
        );

        assert bundleId.getUri().equals("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/SamplingBundle_V1");
    }

    @Test
    public void testGetMetaBundleId() throws IOException {
        var file = Path.of(dataFolder + "dataset2/ProcessingBundle_V0.json");

        var document = deserializeFile(file, Formats.ProvFormat.JSON);

        var cpmDoc = new CpmDocument(document, pF, cPF, cF);

        var metaId = CpmUtils.getMetaBundleId(cpmDoc);

        assert metaId != null;
    }

    @Test
    public void testGetReferencedConnectorId() throws IOException {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");

        var document = deserializeFile(file, Formats.ProvFormat.JSON);

        var cpmDoc = new CpmDocument(document, pF, cPF, cF);

        var connectorNode = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank")
        );

        var referencedConnectorId = CpmUtils.getConnectorIdInReferencedBundle(connectorNode);

        assert referencedConnectorId.getUri().equals(BLANK_URI + "StoredSampleCon_r1");
    }

    @Test
    public void testBundleRequirements() throws IOException {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");

        var document = deserializeFile(file, Formats.ProvFormat.JSON);

        var cpmDoc = new CpmDocument(document, pF, cPF, cF);

        var node1Requirement = new NodeSpecification(
                ".*StoredSampleCon_r1",
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                List.of(ATTR_PROV_TYPE.getUri()),
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                ".*Connector"
                        )
                ));

        var node2Requirement = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                List.of(ATTR_PROV_TYPE.getUri()),
                null);

        LinearSubgraphSpecification subgraphRequirement = new LinearSubgraphSpecification(
                node1Requirement,
                List.of(new EdgeToNodeSpecification(
                        StatementOrBundle.Kind.PROV_GENERATION,
                        null,
                        false,
                        node2Requirement
                ))
        );

        LinearSubgraphSpecification bannedSubgraphRequirement = new LinearSubgraphSpecification(
                node2Requirement,
                List.of(new EdgeToNodeSpecification(
                        StatementOrBundle.Kind.PROV_GENERATION,
                        null,
                        false,
                        node1Requirement
                ))
        );

        BundleSpecification bundleRequirement = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                node1Requirement,
                                EComparisonResult.GREATER_THAN_OR_EQUALS,
                                1
                        ),
                        new CountSpecification(
                                subgraphRequirement,
                                EComparisonResult.GREATER_THAN_OR_EQUALS,
                                1
                        ),
                        new CountSpecification(
                                bannedSubgraphRequirement,
                                EComparisonResult.EQUALS,
                                0
                        )
                )
        );

        var mapper = new ObjectMapper();
        var jsonNode = mapper.valueToTree(bundleRequirement);
        var json = jsonNode.toString();

        var match = bundleRequirement.test(cpmDoc, new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1", "blank"));

        assert match;
    }
    
    
    /*@Test
    public void testSearchByAttributes() throws IOException {
        var nodeAttributes = new NodeAttributes();
        nodeAttributes.attributes = new ArrayList<NodeAttribute>();
        nodeAttributes.attributes.add(new NodeAttribute(
                new org.openprovenance.prov.vanilla.QualifiedName(prov, "type", "prov"),
                new org.openprovenance.prov.vanilla.QualifiedName(cpm, "forwardConnector", "cpm")
        ));
        
        var dto = new NodeAttributesDTO().from(nodeAttributes);
        ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        String serializedTargetSpecification = mapper.writeValueAsString(dto);

        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        var doc = deserializeFile(file, Formats.ProvFormat.JSON);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        var startNode = new org.openprovenance.prov.vanilla.QualifiedName("https://openprovenance.org/blank/", "StoredSampleCon_r1_Spec", "blank");
        var searcher = new BFSBundleNodeSearcher(BFSBundleNodeSearcher::translateNodeSpecsToPredicate);
        
        var result = searcher.search(cpmDoc, startNode, serializedTargetSpecification);
        
        assert result != null;
    }*/

   /* @Test
    public void testSearchById() {
        var doc = TestDocument.getTestDocument1(pF, cPF, cF);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        QualifiedName startNodeId = cPF.newCpmQualifiedName("activity1");
        QualifiedName targetNodeId = cPF.newCpmQualifiedName("entity1");
        INode targetNode = cpmDoc.getNode(targetNodeId);

        var searcher = new BFSBundleNodeSearcher(node -> node.getId().equals(targetNodeId));
        List<INode> results = searcher.search(cpmDoc, startNodeId);

        assert results.size() == 1 && results.contains(targetNode);
    }

    private Object FindValue(INode node, QualifiedName attributeName) {
        for (Element element : node.getElements()) {
            for (Other other : element.getOther()) {
                var otherName = other.getElementName();
                if (attributeName.getLocalPart() == otherName.getLocalPart()
                        && attributeName.getNamespaceURI() == otherName.getNamespaceURI()) {
                    return other.getValue();
                }
            }
        }

        return null;
    }*/

    private Object FindNodeByAttribute(INode node, QualifiedName targetAttribute, Object targetValue) {
        for (Element element : node.getElements()) {
            if (element.getId().equals(targetAttribute)) {
                var found = true;
            }
            for (Other other : element.getOther()) {
                QualifiedName name = other.getElementName();
                if (name.equals(targetAttribute) && Objects.equals(other.getValue(), targetValue)) {
                    return other.getValue();
                }
            }
        }

        return null;
    }

    @Test
    public void FindLocationInNodes() throws IOException {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        var doc = deserializeFile(file, Formats.ProvFormat.JSON);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        var attributeName = new org.openprovenance.prov.vanilla.QualifiedName("http://www.w3.org/ns/prov#", "location", "prov");

        cpmDoc.getNodes().forEach(node -> {
            Object value = CpmUtils.getAttributeValue(node, attributeName);
            if (value != null) {
                System.out.println("Found attribute in node " + node.getId() + ": " + value);
            }
        });
    }

    @Test
    public void FindBackwardConnectors() throws IOException {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        var doc = deserializeFile(file, Formats.ProvFormat.JSON);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        var attributeName = new org.openprovenance.prov.vanilla.QualifiedName("http://www.w3.org/ns/prov#", "type", "prov");
        var attributeValue = new org.openprovenance.prov.vanilla.QualifiedName(CPM_URI, "backwardConnector", "cpm");

        cpmDoc.getNodes().forEach(node -> {
            Object value = CpmUtils.getAttributeValue(node, attributeName);
            if (value != null) {
                System.out.println("Found attribute in node " + node.getId() + ": " + value);
            }
        });
    }

    @Test
    public void getBundleId() {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        try {
            var doc = deserializeFile(file, Formats.ProvFormat.JSON);
            var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
            var bundleId = cpmDoc.getBundleId();
            System.out.println("Bundle ID: " + bundleId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
