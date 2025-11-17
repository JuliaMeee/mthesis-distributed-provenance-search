import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.bundleVersionPicker.pickerImplementations.LatestVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.pickerImplementations.SpecifiedVersionPicker;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;

import static cz.muni.xmichalk.util.ProvDocumentUtils.deserializeFile;

public class VersionPickerTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";


    @Test
    public void testPickNewestVersion() throws IOException {
        Path file = Path.of(dataFolder + "metaDocument.json");

        Document document = deserializeFile(file, Formats.ProvFormat.JSON);

        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        QualifiedName bundleId = LatestVersionPicker.pickFrom(
                new org.openprovenance.prov.vanilla.QualifiedName("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/", "SamplingBundle_V0", "storage"),
                cpmDoc
        );

        assert bundleId.getUri().equals("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/SamplingBundle_V1");
    }

    @Test
    public void testPickReferenced() {
        QualifiedName bundleId = new SpecifiedVersionPicker().apply(
                new org.openprovenance.prov.vanilla.QualifiedName("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/", "SamplingBundle_V0", "storage")
        );

        assert bundleId.getUri().equals("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/SamplingBundle_V0");
    }
}