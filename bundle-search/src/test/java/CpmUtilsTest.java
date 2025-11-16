import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
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

import static cz.muni.xmichalk.util.NameSpaceConstants.BLANK_URI;
import static cz.muni.xmichalk.util.ProvDocumentUtils.deserializeFile;

public class CpmUtilsTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";

    @Test
    public void testGetMetaBundleId() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V0.json");

        Document document = deserializeFile(file, Formats.ProvFormat.JSON);

        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        QualifiedName metaId = CpmUtils.getMetaBundleId(cpmDoc);

        assert metaId != null;
    }

    @Test
    public void testGetReferencedConnectorId() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");

        Document document = deserializeFile(file, Formats.ProvFormat.JSON);

        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        INode connectorNode = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank")
        );

        QualifiedName referencedConnectorId = CpmUtils.getConnectorIdInReferencedBundle(connectorNode);

        assert referencedConnectorId.getUri().equals(BLANK_URI + "StoredSampleCon_r1");
    }

    @Test
    public void testGetLocation() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document doc = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                "http://www.w3.org/ns/prov#", "location", "prov");

        List<Object> foundLocations = new ArrayList<Object>();

        cpmDoc.getNodes().forEach(node -> {
            Object value = CpmUtils.getAttributeValue(node, attributeName);
            if (value != null) {
                if (value instanceof List<?> listValue) {
                    if (!listValue.isEmpty()) {
                        foundLocations.add(value);
                    }
                }
            }
        });

        assert (foundLocations.size() == 6);
    }

    @Test
    public void testHasAttributeValue() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document doc = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        INode node = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(
                        BLANK_URI, "Sampling", "blank")
        );
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                NameSpaceConstants.PROV_URI, "type", "prov");

        boolean hasValue = CpmUtils.hasAttributeTargetValue(
                node,
                attributeName,
                QualifiedName.class,
                qn -> qn.getUri().equals(NameSpaceConstants.CPM_URI + "mainActivity")
        );

        assert hasValue;
    }

    @Test
    public void testDoesNotHaveAttributeValue() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document doc = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        INode node = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(
                        BLANK_URI, "Sampling", "blank")
        );
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                NameSpaceConstants.PROV_URI, "type", "prov");

        boolean hasValue = CpmUtils.hasAttributeTargetValue(
                node,
                attributeName,
                QualifiedName.class,
                qn -> qn.getUri().equals(NameSpaceConstants.CPM_URI + "backwardConnector")
        );

        assert !hasValue;
    }
}
