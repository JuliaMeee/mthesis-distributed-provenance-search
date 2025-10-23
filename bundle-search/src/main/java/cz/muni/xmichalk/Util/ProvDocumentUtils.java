package cz.muni.xmichalk.Util;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.model.interop.InteropMediaType;
import org.openprovenance.prov.vanilla.HasAttributes;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cz.muni.xmichalk.Util.Constants.BLANK_URI;

public class ProvDocumentUtils {
    public static final Charset charset = java.nio.charset.StandardCharsets.UTF_8;
    
    public static String serialize(Document document, Formats.ProvFormat format) {
        var interop = new InteropFramework();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        interop.writeDocument(outputStream, document, InteropMediaType.MEDIA_APPLICATION_JSON, false);
        return outputStream.toString(charset);
    }

    public static Document deserialize(String serialized, Formats.ProvFormat format) throws IOException {
        serialized = prepareForDeserialization(serialized, format);
        var interop = new InteropFramework();
        InputStream inputStream = new ByteArrayInputStream(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
            serializedDocument = ProvJsonUtils.copyOuterPrefixesIntoBundles(serializedDocument);
        }

        return serializedDocument;
    }
    
    public static Document encapsulateInDocument(List<INode> nodes) {
        ProvFactory pf = ProvFactory.getFactory();
        Document doc = pf.newDocument();
        
        Namespace ns = pf.newNamespace();

        var bundle = pf.newNamedBundle(pf.newQualifiedName(BLANK_URI, "anonymous_encapsulating_bundle", "blank"), null);

        for (INode node : nodes) {
            var elements = node.getElements();

            for (var element : elements) {
                bundle.getStatement().add(element);
            }
        }
        
        registerAllNamespaces(ns, bundle);
        doc.setNamespace(ns);
        doc.getStatementOrBundle().add(bundle);
        
        return doc;
    }
    
    public static QualifiedName getBundleId(Document document) {
        for (Object o : document.getStatementOrBundle()) {
            if (o instanceof final Bundle bundle) {
                return bundle.getId();
            }
        }
        
        return null;
    }
    
    public static Namespace registerAllNamespaces(Namespace namespace, Object provObject) {
        if (provObject instanceof Document doc) {
            for (StatementOrBundle statement : doc.getStatementOrBundle()) {
                registerAllNamespaces(namespace, statement);
            }
        } else if (provObject instanceof Bundle bundle) {
            QualifiedName bundleId = bundle.getId();
            if (bundleId != null) {
                namespace.register(bundleId.getPrefix(), bundleId.getNamespaceURI());
            }
            for (Statement statement : bundle.getStatement()) {
                registerAllNamespaces(namespace, statement);
            }
        } else if (provObject instanceof Identifiable identifiable) {
            QualifiedName id = identifiable.getId();
            if (id != null) {
                namespace.register(id.getPrefix(), id.getNamespaceURI());
            }
        }

        if (provObject instanceof HasAttributes hasAttributes) {
            for (Attribute attr : hasAttributes.getAttributes()) {
                QualifiedName attrName = attr.getElementName();
                if (attrName != null) {
                    namespace.register(attrName.getPrefix(), attrName.getNamespaceURI());
                }
                
                Object value = attr.getValue();
                if (value instanceof QualifiedName qnValue) {
                    namespace.register(qnValue.getPrefix(), qnValue.getNamespaceURI());
                }
            }
        }
        
        return namespace;
    }
}
