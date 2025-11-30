import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ProvDocumentUtilsTest {
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

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("serializationRoundTripParams")
    public void testSerializationRoundTrip(CpmDocument cpmDocument, Formats.ProvFormat format) throws IOException {
        QualifiedName bundleId = cpmDocument.getBundleId();
        String id = bundleId.getLocalPart();

        Path filePath1 = Path.of(dataFolder, id + "_1" + getExtension(format));
        ProvDocumentUtils.serializeIntoFile(filePath1, cpmDocument.toDocument(), format);
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

    static Stream<Object[]> serializationRoundTripParams() {
        return Stream.of(
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.JSON},
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.JSONLD},
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.PROVN},
                /* Failing tests
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.RDFXML},
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.TURTLE},
                new Object[]{TestDocumentProvider.testDocument1, Formats.ProvFormat.TRIG},*/

                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.JSON},
                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.JSONLD},
                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.PROVN}
                /* Failing tests
                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.RDFXML},
                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.TURTLE},
                new Object[]{TestDocumentProvider.testDocument2, Formats.ProvFormat.TRIG}*/
        );
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("serializationRoundTripParams")
    public void testEncapsulateInDocument(CpmDocument cpmDocument) {
        Document newDocument = ProvDocumentUtils.encapsulateInDocument(cpmDocument.getNodes(), cpmDocument.getEdges());
        CpmDocument newCpmDocument = new CpmDocument(newDocument, pF, cPF, cF);

        assert (newCpmDocument.getNodes().size() == cpmDocument.getNodes().size());
        assert (newCpmDocument.getEdges().size() == cpmDocument.getEdges().size());
        assert (newCpmDocument.getNodes().stream().allMatch(node -> cpmDocument.getNodes(node.getId()) != null));
    }

    static Stream<Object[]> encapsulateInDocumentParams() {
        return Stream.of(
                new Object[]{TestDocumentProvider.samplingBundle_V0},
                new Object[]{TestDocumentProvider.samplingBundle_V1},
                new Object[]{TestDocumentProvider.samplingBundle_V0_meta},
                new Object[]{TestDocumentProvider.processingBundle_V0},
                new Object[]{TestDocumentProvider.processingBundle_V1},
                new Object[]{TestDocumentProvider.processingBundle_V0_meta}
        );
    }

}
