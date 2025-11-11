import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleVersionPicker.PickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.Util.CpmUtils;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static cz.muni.xmichalk.Util.NameSpaceConstants.BLANK_URI;
import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserializeFile;

public class BundleSearcherTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ProvUtilities u = new ProvUtilities();
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";


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
    public void findLocationInNodes() throws IOException {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        var doc = deserializeFile(file, Formats.ProvFormat.JSON);
        var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
        var attributeName = new org.openprovenance.prov.vanilla.QualifiedName("http://www.w3.org/ns/prov#", "location", "prov");

        List<Object> foundLocations = new ArrayList<Object>();

        cpmDoc.getNodes().forEach(node -> {
            Object value = CpmUtils.getAttributeValue(node, attributeName);
            if (value != null) {
                if (value instanceof final List<?> listValue) {
                    if (!listValue.isEmpty()) {
                        foundLocations.add(value);
                    }
                }
            }
        });

        assert (foundLocations.size() == 6);
    }

    @Test
    public void getBundleId() {
        var file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        try {
            var doc = deserializeFile(file, Formats.ProvFormat.JSON);
            var cpmDoc = new CpmDocument(doc, pF, cPF, cF);
            var bundleId = cpmDoc.getBundleId();

            assert (bundleId.getUri().equals("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/SamplingBundle_V1"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
