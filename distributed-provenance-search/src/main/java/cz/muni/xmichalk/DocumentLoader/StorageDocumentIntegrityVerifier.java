package cz.muni.xmichalk.DocumentLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.Token;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class StorageDocumentIntegrityVerifier {

    public boolean verifySignature(String encodedDocument, Token token) {
        try {
            String payload = getVerifySignaturePayload(encodedDocument, token);

            String url = getVerifySignatureUrl(token);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(url);
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

                try (CloseableHttpResponse response = client.execute(post)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    return statusCode == 200;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Integrity check failed", e);
        }

    }

    private static String getVerifySignatureUrl(Token token) {
        /*return storageResponse.token.data.additionalData.trustedPartyUri + "/api/v1/verifySignature";*/
        return "http://localhost:8020/api/v1/verifySignature"; // TODO change to docker service url
    }

    private static String getVerifySignaturePayload(String document, Token token) throws JsonProcessingException {
        String organizationId = token.data.originatorId;
        String signature = token.signature;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("organizationId", organizationId);
        jsonNode.put("document", document);
        jsonNode.put("signature", signature);
        return mapper.writeValueAsString(jsonNode);
    }

    public boolean verifyHash(String serializedDocument, String hashFunctionName, String expectedHash) {
        MessageDigest hashFunction = getHashFunction(hashFunctionName);
        byte[] hash = hashString(serializedDocument, hashFunction);
        String hashHex = bytesToHex(hash);
        return hashHex.equals(expectedHash);
    }

    public MessageDigest getHashFunction(String hashFunction) {
        try {
            return MessageDigest.getInstance(hashFunction);
        } catch (Exception e) {
            throw new RuntimeException("Unsupported hash function: " + hashFunction, e);
        }
    }

    public static byte[] hashString(String input, MessageDigest digest) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        digest.update(inputBytes);
        return digest.digest();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
