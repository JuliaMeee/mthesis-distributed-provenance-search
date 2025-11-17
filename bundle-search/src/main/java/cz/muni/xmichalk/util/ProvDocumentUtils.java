package cz.muni.xmichalk.util;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.model.interop.InteropMediaType;
import org.openprovenance.prov.vanilla.HasAttributes;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static cz.muni.xmichalk.util.NameSpaceConstants.BLANK_URI;

public class ProvDocumentUtils {
    public static final Charset charset = java.nio.charset.StandardCharsets.UTF_8;

    public static String serialize(Document document, Formats.ProvFormat format) {
        InteropFramework interop = new InteropFramework();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        interop.writeDocument(outputStream, document, ProvFormatToIntermediaType(format), false);
        return outputStream.toString(charset);
    }

    public static void serializeIntoFile(Path filePath, Document document, Formats.ProvFormat format) throws IOException {
        String string = serialize(document, format);
        Files.writeString(filePath, string, charset);
    }

    public static Document deserialize(String serialized, Formats.ProvFormat format) throws IOException {
        serialized = prepareForDeserialization(serialized, format);
        InteropFramework interop = new InteropFramework();
        InputStream inputStream = new ByteArrayInputStream(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return interop.readDocument(inputStream, format);
    }

    public static Document deserializeFile(Path filePath, Formats.ProvFormat format) throws IOException {
        String serialized = Files.readString(filePath, StandardCharsets.UTF_8);
        return deserialize(serialized, format);
    }

    public static String prepareForDeserialization(String serializedDocument, Formats.ProvFormat format) {
        if (format == Formats.ProvFormat.JSON) {
            serializedDocument = ProvJsonUtils.prepareJsonForDeserialization(serializedDocument);
        }

        return serializedDocument;
    }

    public static Document encapsulateInDocument(List<INode> nodes) {
        ProvFactory pf = ProvFactory.getFactory();
        Document doc = pf.newDocument();

        Namespace ns = pf.newNamespace();

        Bundle bundle = pf.newNamedBundle(pf.newQualifiedName(BLANK_URI, "anonymous_encapsulating_bundle", "blank"), null);

        for (INode node : nodes) {
            List<Element> elements = node.getElements();

            for (Element element : elements) {
                bundle.getStatement().add(element);
            }
        }

        registerAllPrefixes(ns, bundle);
        doc.setNamespace(ns);
        doc.getStatementOrBundle().add(bundle);

        return doc;
    }

    public static QualifiedName getBundleId(Document document) {
        for (Object o : document.getStatementOrBundle()) {
            if (o instanceof Bundle bundle) {
                return bundle.getId();
            }
        }

        return null;
    }

    public static void doForEachQualifiedName(Object provObject, Consumer<QualifiedName> action) {
        if (provObject instanceof Document doc) {
            for (StatementOrBundle statement : doc.getStatementOrBundle()) {
                doForEachQualifiedName(statement, action);
            }
        } else if (provObject instanceof Bundle bundle) {
            QualifiedName bundleId = bundle.getId();
            if (bundleId != null) {
                action.accept(bundleId);
            }
            for (Statement statement : bundle.getStatement()) {
                doForEachQualifiedName(statement, action);
            }
        } else if (provObject instanceof Identifiable identifiable) {
            QualifiedName id = identifiable.getId();
            if (id != null) {
                action.accept(id);
            }
        }

        if (provObject instanceof HasAttributes hasAttributes) {
            for (Attribute attr : hasAttributes.getAttributes()) {
                QualifiedName attrName = attr.getElementName();
                if (attrName != null) {
                    action.accept(attrName);
                }

                Object value = attr.getValue();
                if (value instanceof QualifiedName qnValue) {
                    action.accept(qnValue);
                }

                if (value instanceof List<?> listValue) {
                    for (Object item : listValue) {
                        if (item instanceof QualifiedName qnItem) {
                            action.accept(qnItem);
                        }
                    }
                }
            }
        }
    }

    public static void registerAllPrefixes(Namespace namespace, Object provObject) {
        doForEachQualifiedName(
                provObject,
                qn -> namespace.register(qn.getPrefix(), qn.getNamespaceURI()));
    }

    public static String ProvFormatToIntermediaType(Formats.ProvFormat format) {
        return switch (format) {
            case JSON -> InteropMediaType.MEDIA_APPLICATION_JSON;
            case JSONLD -> InteropMediaType.MEDIA_APPLICATION_JSONLD;
            case PROVN -> InteropMediaType.MEDIA_TEXT_PROVENANCE_NOTATION;
            case TURTLE -> InteropMediaType.MEDIA_TEXT_TURTLE;
            case TRIG -> InteropMediaType.MEDIA_APPLICATION_TRIG;
            case RDFXML -> InteropMediaType.MEDIA_APPLICATION_XML;
            default -> throw new IllegalStateException("Switch case for: " + format + " is not defined.");
        };
    }
}
