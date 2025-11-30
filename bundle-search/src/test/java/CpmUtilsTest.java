import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.util.AttributeUtils;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.NameSpaceConstants;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cz.muni.xmichalk.util.NameSpaceConstants.BLANK_URI;

public class CpmUtilsTest extends TestDocumentProvider {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";

    public CpmUtilsTest() throws IOException {
    }

    @Test
    public void testGetMetaBundleId() throws IOException {
        CpmDocument cpmDoc = processingBundle_V0;

        QualifiedName metaId = CpmUtils.getMetaBundleId(cpmDoc);

        assert metaId != null;
    }

    @Test
    public void testGetReferencedConnectorId() throws IOException {
        CpmDocument cpmDoc = samplingBundle_V1;

        INode connectorNode = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(BLANK_URI, "StoredSampleCon_r1_Spec", "blank")
        );

        QualifiedName referencedConnectorId = CpmUtils.getConnectorIdInReferencedBundle(connectorNode);

        assert referencedConnectorId.getUri().equals(BLANK_URI + "StoredSampleCon_r1");
    }

    @Test
    public void testGetLocation() throws IOException {
        CpmDocument cpmDoc = samplingBundle_V1;
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                "http://www.w3.org/ns/prov#", "location", "prov");

        List<Object> foundLocations = new ArrayList<Object>();

        cpmDoc.getNodes().forEach(node -> {
            Object value = AttributeUtils.getAttributeValue(node, attributeName);
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
        CpmDocument cpmDoc = samplingBundle_V1;
        INode node = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(
                        BLANK_URI, "Sampling", "blank")
        );
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                NameSpaceConstants.PROV_URI, "type", "prov");

        boolean hasValue = AttributeUtils.hasAttributeTargetValue(
                node,
                attributeName,
                QualifiedName.class,
                qn -> qn.getUri().equals(NameSpaceConstants.CPM_URI + "mainActivity")
        );

        assert hasValue;
    }

    @Test
    public void testDoesNotHaveAttributeValue() throws IOException {
        CpmDocument cpmDoc = samplingBundle_V1;
        INode node = cpmDoc.getNode(
                new org.openprovenance.prov.vanilla.QualifiedName(
                        BLANK_URI, "Sampling", "blank")
        );
        QualifiedName attributeName = new org.openprovenance.prov.vanilla.QualifiedName(
                NameSpaceConstants.PROV_URI, "type", "prov");

        boolean hasValue = AttributeUtils.hasAttributeTargetValue(
                node,
                attributeName,
                QualifiedName.class,
                qn -> qn.getUri().equals(NameSpaceConstants.CPM_URI + "backwardConnector")
        );

        assert !hasValue;
    }
}
