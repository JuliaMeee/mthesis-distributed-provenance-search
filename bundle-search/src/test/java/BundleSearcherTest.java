import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearch.General.NodeAttributeSearcher;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static cz.muni.xmichalk.Util.Constants.CPM_URI;
import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserializeFile;

public class BundleSearcherTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ProvUtilities u = new ProvUtilities();
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";
    
    @Test
    public void testLoadPartialNode(){
        var file = Path.of(dataFolder + "nodeSpecs.json");
        try {
            var doc = deserializeFile(file, Formats.ProvFormat.JSON);
            var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
            assert cpmDoc.getNodes().size() == 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            Object value = new NodeAttributeSearcher().tryGetValue(node, attributeName);
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
            Object value = new NodeAttributeSearcher().tryGetValue(node, attributeName);
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
