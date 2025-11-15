package cz.muni.xmichalk.documentLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.xmichalk.documentLoader.storageDTO.GetDocumentResponse;
import cz.muni.xmichalk.documentLoader.storageDTO.GetMetaResponse;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StorageDocumentLoader implements IDocumentLoader {
    private static final Formats.ProvFormat FORMAT = Formats.ProvFormat.JSON;
    private static final String FORMAT_QUERY_PARAM = "format=json";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final ProvFactory provFactory;
    private final ICpmFactory cpmFactory;
    private final ICpmProvFactory cpmProvFactory;

    public StorageDocumentLoader(ProvFactory provFactory, ICpmFactory cpmFactory, ICpmProvFactory cpmProvFactory) {
        this.provFactory = provFactory;
        this.cpmFactory = cpmFactory;
        this.cpmProvFactory = cpmProvFactory;
    }

    @Override
    public StorageDocument loadDocument(String uri) {
        try {
            uri += (uri.contains("?") ? "&" : "?") + FORMAT_QUERY_PARAM;
            String responseBody = getRequest(uri);
            ObjectMapper mapper = new ObjectMapper();
            GetDocumentResponse storageResponse = mapper.readValue(responseBody, GetDocumentResponse.class);
            String decodedDocument = decodeData(storageResponse.document);
            Document document = ProvDocumentUtils.deserialize(decodedDocument, FORMAT);
            return new StorageDocument(document, storageResponse.token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document " + uri, e);
        }
    }

    @Override
    public StorageCpmDocument loadCpmDocument(String uri) {
        return toCpmDocument(loadDocument(uri));
    }

    @Override
    public StorageDocument loadMetaDocument(String uri) {
        try {
            uri += (uri.contains("?") ? "&" : "?") + FORMAT_QUERY_PARAM;
            String responseBody = getRequest(uri);
            ObjectMapper mapper = new ObjectMapper();
            GetMetaResponse storageResponse = mapper.readValue(responseBody, GetMetaResponse.class);
            String decodedDocument = decodeData(storageResponse.graph);
            Document document = ProvDocumentUtils.deserialize(decodedDocument, FORMAT);
            return new StorageDocument(document, storageResponse.token);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document " + uri, e);
        }
    }

    @Override
    public StorageCpmDocument loadMetaCpmDocument(String uri) {
        return toCpmDocument(loadMetaDocument(uri));

    }

    public StorageCpmDocument toCpmDocument(StorageDocument storageDocument) {
        if (storageDocument == null) {
            return null;
        }
        if (storageDocument.document == null) {
            return new StorageCpmDocument(null, storageDocument.token);
        }

        return new StorageCpmDocument(
                new CpmDocument(storageDocument.document, provFactory, cpmProvFactory, cpmFactory),
                storageDocument.token
        );
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
}
