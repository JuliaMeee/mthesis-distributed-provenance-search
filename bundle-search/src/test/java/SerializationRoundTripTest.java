import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;

public class SerializationRoundTripTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/serialization/";

    public static String getExtension(Formats.ProvFormat format) {
        return switch (format) {
            case RDFXML -> ".xml";
            case JSON -> ".json";
            case JSONLD -> ".jsonld";
            case PROVN -> ".provn";
            case TURTLE -> ".ttl";
            case TRIG -> ".trig";
            default -> ".txt";
        };
    }

    public void testFormat(Document document, Formats.ProvFormat format) throws IOException {
        QualifiedName bundleId = ProvDocumentUtils.getBundleId(document);
        String id = bundleId.getLocalPart();

        Path filePath1 = Path.of(dataFolder, id + "_1" + getExtension(format));
        ProvDocumentUtils.serializeIntoFile(filePath1, document, format);
        Document deserializedDocument1 = ProvDocumentUtils.deserializeFile(filePath1, format);
        CpmDocument cpmDocument1 = new CpmDocument(deserializedDocument1, pF, cPF, cF);

        Path filePath2 = Path.of(dataFolder, id + "_2" + getExtension(format));
        ProvDocumentUtils.serializeIntoFile(filePath2, deserializedDocument1, format);
        Document deserializedDocument2 = ProvDocumentUtils.deserializeFile(filePath2, format);
        CpmDocument cpmDocument2 = new CpmDocument(deserializedDocument2, pF, cPF, cF);

        assert (cpmDocument1.getNodes().size() == cpmDocument2.getNodes().size());
        assert (cpmDocument1.getEdges().size() == cpmDocument2.getEdges().size());
        assert (cpmDocument1.getBundleId().equals(cpmDocument2.getBundleId()));
        assert (cpmDocument1.getBundleId().equals(bundleId));
    }

    @Test
    public void testMultipleFormats() {
        Document[] documents = {
                TestDocumentProvider.testDocument1.toDocument(),
                TestDocumentProvider.testDocument2.toDocument(),
        };

        Formats.ProvFormat[] formats = {
                Formats.ProvFormat.JSON,
                Formats.ProvFormat.JSONLD,
                Formats.ProvFormat.PROVN,
        };

        boolean allTestsPassed = true;

        for (Document document : documents) {
            for (Formats.ProvFormat format : formats) {
                try {
                    testFormat(document, format);
                } catch (Exception e) {
                    System.err.println("Error testing format " + format +
                            " for document with bundle ID " + ProvDocumentUtils.getBundleId(document).getLocalPart() + ": " + e);
                    allTestsPassed = false;
                }
            }
        }

        assert (allTestsPassed);
    }
}
