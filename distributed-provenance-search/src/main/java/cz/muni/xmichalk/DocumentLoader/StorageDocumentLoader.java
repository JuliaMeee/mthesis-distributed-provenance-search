package cz.muni.xmichalk.DocumentLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.StorageResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StorageDocumentLoader implements IDocumentLoader {
    ProvFactory pF;
    ICpmFactory cF;
    ICpmProvFactory cPF;
    Formats.ProvFormat format;

    public StorageDocumentLoader(ProvFactory pF, ICpmFactory cF, ICpmProvFactory cPF, Formats.ProvFormat format) {
        this.pF = pF;
        this.cF = cF;
        this.cPF = cPF;
        this.format = format;
    }

    @Override
    public CpmDocument load(String uri) {
        try {
            StorageResponse storageResponse = fetchDocument(getLocalhostUri(uri));
            String decodedDocument = decodeData(storageResponse.document);
            Document document = deserialize(decodedDocument, format);
            return new CpmDocument(document, pF, cPF, cF);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve document " + uri, e);
        }
    }

    private static StorageResponse fetchDocument(String uri) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Unexpected response code: " + statusCode);
                }
                String responseJson = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(responseJson, StorageResponse.class);
            }
        }
    }

    private static String decodeData(String base64Data) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private static Document deserialize(String serializedDocument, Formats.ProvFormat format) throws IOException {
        if (format == Formats.ProvFormat.JSON) {
            serializedDocument = addExplicitBundleId(serializedDocument);
        }

        var inputStream = new ByteArrayInputStream(serializedDocument.getBytes(StandardCharsets.UTF_8));
        var interop = new InteropFramework();
        return interop.readDocument(inputStream, format);
    }

    /**
     * Add explicit "@id" property to bundle to comply with provtoolbox deserialization requirements.
     *
     * @param json the original JSON string possibly without "@id" in bundle
     * @return the modified JSON string in minimized format with proper "@id" added to bundle
     */
    private static String addExplicitBundleId(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(json);

            var bundleNode = root.path("bundle");
            if (bundleNode.isObject()) {
                var fieldNames = bundleNode.fieldNames();
                if (fieldNames.hasNext()) {
                    String bundleId = fieldNames.next();
                    var bundleObj = (ObjectNode) bundleNode.path(bundleId);
                    if (!bundleObj.has("@id")) {
                        bundleObj.put("@id", bundleId);
                    }
                }
            }
            return mapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to add explicit bundle id", e);
        }
    }

    private static String getLocalhostUri(String uri) {
        return uri.replaceFirst("prov-storage-(\\d):8000", "localhost:800$1");
    }
}
