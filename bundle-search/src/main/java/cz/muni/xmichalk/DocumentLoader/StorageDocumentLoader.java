package cz.muni.xmichalk.DocumentLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StorageDocumentLoader implements IDocumentLoader {
    private static final Formats.ProvFormat FORMAT = Formats.ProvFormat.JSON;
    private static final String FORMAT_QUERY_PARAM = "format=json";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    public StorageDocument loadDocument(String uri) {
        try {
            uri += (uri.contains("?") ? "&" : "?") + FORMAT_QUERY_PARAM;
            String responseBody = getRequest(uri);
            ObjectMapper mapper = new ObjectMapper();
            GetDocumentResponse storageResponse = mapper.readValue(responseBody, GetDocumentResponse.class);
            String decodedDocument = decodeData(storageResponse.document);
            Document document = deserialize(decodedDocument, FORMAT);
            return new StorageDocument(document, storageResponse.token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document " + uri, e);
        }
    }

    @Override
    public StorageDocument loadMetaDocument(String uri) {
        try {
            uri += (uri.contains("?") ? "&" : "?") + FORMAT_QUERY_PARAM;
            String responseBody = getRequest(uri);
            ObjectMapper mapper = new ObjectMapper();
            GetMetaResponse storageResponse = mapper.readValue(responseBody, GetMetaResponse.class);
            String decodedDocument = decodeData(storageResponse.graph);
            Document document = deserialize(decodedDocument, FORMAT);
            return new StorageDocument(document, storageResponse.token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document " + uri, e);
        }
    }

    private static String getRequest(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
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
        return new String(decodedBytes, CHARSET);
    }

    private static Document deserialize(String serializedDocument, Formats.ProvFormat format) throws IOException {
        serializedDocument = prepareForDeserialization(serializedDocument, format);

        var inputStream = new ByteArrayInputStream(serializedDocument.getBytes(CHARSET));
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
}
