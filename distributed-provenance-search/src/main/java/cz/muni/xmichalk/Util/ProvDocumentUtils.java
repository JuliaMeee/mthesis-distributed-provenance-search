package cz.muni.xmichalk.Util;

import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProvDocumentUtils {
    public static final Charset charset = StandardCharsets.UTF_8;

    public static String serialize(Document document, Formats.ProvFormat format) {
        var interop = new InteropFramework();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        interop.writeDocument(outputStream, document, format);
        return outputStream.toString(charset);
    }

    public static Document deserialize(String serialized, Formats.ProvFormat format) throws IOException {
        serialized = prepareForDeserialization(serialized, format);
        var interop = new InteropFramework();
        InputStream inputStream = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
        return interop.readDocument(inputStream, format);
    }

    public static Document deserializeFile(Path filePath, Formats.ProvFormat format) throws IOException {
        String serialized = Files.readString(filePath, StandardCharsets.UTF_8);
        return deserialize(serialized, format);
    }

    public static String prepareForDeserialization(String serializedDocument, Formats.ProvFormat format) {
        if (format == Formats.ProvFormat.JSON) {
            serializedDocument = ProvJsonUtils.addExplicitBundleId(serializedDocument);
            serializedDocument = ProvJsonUtils.stringifyValues(serializedDocument);
        }

        return serializedDocument;
    }
}
