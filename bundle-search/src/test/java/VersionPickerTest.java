import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.bundleVersionPicker.implementations.LatestVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.implementations.SpecifiedVersionPicker;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.QualifiedName;

import java.io.IOException;

public class VersionPickerTest extends TestDocumentProvider {
    public VersionPickerTest() throws IOException {
    }


    @Test
    public void testPickNewestVersion() {
        CpmDocument metaCpmDoc = samplingBundleMeta;

        INode latestVersionNode = LatestVersionPicker.pickLatestVersionNode(metaCpmDoc);

        assert latestVersionNode.getId().getUri().equals(
                samplingBundle_V1.getBundleId().getUri());
    }

    @Test
    public void testPickSpecified() {
        CpmDocument cpmDoc = samplingBundle_V0;

        QualifiedName bundleId = new SpecifiedVersionPicker().apply(
                cpmDoc
        );

        assert bundleId.getUri().equals(samplingBundle_V0.getBundleId().getUri());
    }
}