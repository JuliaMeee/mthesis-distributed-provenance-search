import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearcher.BFSBundleNodeSearcher;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserializeFile;

public class BundleSearcherTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ProvUtilities u = new ProvUtilities();
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";

    @Test
    public void testInvalidStartNodeId() {
        var doc = TestDocument.getTestDocument1(pF, cPF, cF);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        QualifiedName invalidStartNodeId = cPF.newCpmQualifiedName("invalidId");

        var searcher = new BFSBundleNodeSearcher(node -> true);
        List<INode> results = searcher.search(cpmDoc, invalidStartNodeId);

        assert results.isEmpty();
    }

    @Test
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
    }


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

    /*@Test
    public void FindInBundle() throws IOException {
        var file = dataFolder + "dataset1/SamplingBundle_V1.json";
        var doc = new SerializationRoundTripTest().deserialize(file, Formats.ProvFormat.JSON);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        var attributeName = new org.openprovenance.prov.vanilla.QualifiedName("http://www.w3.org/ns/prov#", "location", "prov");

        cpmDoc.getNodes().forEach(node -> {
            Object value = new NodeSearcher().tryGetValue(node, attributeName);
            if (value != null) {
                System.out.println("Found attribute in node " + node.getId() + ": " + value);
            }
        });
    }*/
}
