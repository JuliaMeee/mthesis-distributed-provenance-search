import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SerializationRoundTripTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/serialization/";

    public QualifiedName getBundleId(Document document) {
        Bundle bundle = (Bundle) document.getStatementOrBundle().get(0);
        return bundle.getId();
    }

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

    public String serialize(Document document, Formats.ProvFormat format) {
        var filePath = dataFolder + getBundleId(document).getLocalPart() + "_" + format.toString() + getExtension(format);
        var interop = new InteropFramework();
        interop.writeDocument(filePath, document, format);
        return filePath;
    }

    public Document deserialize(String filePath, Formats.ProvFormat format) throws IOException {
        var interop = new InteropFramework();
        InputStream inputStream = new FileInputStream(filePath);
        return interop.readDocument(inputStream, format);
    }

    public CpmDocument testFormat(Document document, Formats.ProvFormat format) throws IOException {
        String filePath = serialize(document, format);
        Document deserializedDocument = deserialize(filePath, format);
        return new CpmDocument(deserializedDocument, pF, cPF, cF);
    }

    @Test
    public void testMultipleFormats() throws IOException, DatatypeConfigurationException {
        Document[] documents = {
                TestDocument.getTestDocument1(pF, cPF, cF),
                TestDocument.getTestDocument2(pF, cPF, cF),
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
                    System.err.println("Error testing format: " + format +
                            " for document with bundle ID: " + getBundleId(document).getLocalPart());
                    e.printStackTrace();
                    allTestsPassed = false;
                }
            }
        }

        assert (allTestsPassed);
    }
}
