package cz.muni.xmichalk.DocumentLoader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.GetDocumentResponse;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.GetMetaResponse;
import cz.muni.xmichalk.Util.ProvJsonUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StorageDocumentLoader implements IDocumentLoader {
    private static final Formats.ProvFormat FORMAT = Formats.ProvFormat.JSON;
    private static final String FORMAT_QUERY_PARAM = "format=json";

    @Override
    public DocumentWithIntegrity loadDocument(String uri) {
        try {
            String responseJson = fetchDocument(getLocalhostUri(uri));
            ObjectMapper mapper = new ObjectMapper();
            GetDocumentResponse storageResponse = mapper.readValue(responseJson, GetDocumentResponse.class);
            String decodedDocument = decodeData(storageResponse.document);
            Document document = deserialize(decodedDocument, FORMAT);
            boolean integrity = new StorageDocumentIntegrityVerifier().verifySignature(storageResponse.document, storageResponse.token);
            return new DocumentWithIntegrity(document, integrity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve document " + uri, e);
        }
    }

    @Override
    public DocumentWithIntegrity loadMetaDocument(String uri) {
        try {
            String responseJson = fetchDocument(getLocalhostUri(uri));
            ObjectMapper mapper = new ObjectMapper();
            GetMetaResponse storageResponse = mapper.readValue(responseJson, GetMetaResponse.class);
            String decodedDocument = decodeData(storageResponse.graph);
            Document document = deserialize(decodedDocument, FORMAT);
            boolean integrity = new StorageDocumentIntegrityVerifier().verifySignature(storageResponse.graph, storageResponse.token);
            return new DocumentWithIntegrity(document, integrity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve document " + uri, e);
        }
    }

    private static String fetchDocument(String uri) throws IOException {
        uri += (uri.contains("?") ? "&" : "?") + FORMAT_QUERY_PARAM;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Unexpected response code: " + statusCode);
                }
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private static String decodeData(String base64Data) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private static Document deserialize(String serializedDocument, Formats.ProvFormat format) throws IOException {
        serializedDocument = prepareForDeserialization(serializedDocument, format);

        var inputStream = new ByteArrayInputStream(serializedDocument.getBytes(StandardCharsets.UTF_8));
        var interop = new InteropFramework();
        return interop.readDocument(inputStream, format);
    }

    private static String prepareForDeserialization(String serializedDocument, Formats.ProvFormat format) {
        if (format == Formats.ProvFormat.JSON) {
            serializedDocument = ProvJsonUtils.addExplicitBundleId(serializedDocument);
            serializedDocument = ProvJsonUtils.stringifyValues(serializedDocument);
        }

        return serializedDocument;
    }

    public static String getBundleMetaUri(String bundleUri) {
        return bundleUri.replace("organizations/ORG1/documents/", "/documents/meta/") + "_meta";
    }

    private static String getLocalhostUri(String uri) {
        return uri.replaceFirst("prov-storage-(\\d):8000", "localhost:800$1");
    }
}
