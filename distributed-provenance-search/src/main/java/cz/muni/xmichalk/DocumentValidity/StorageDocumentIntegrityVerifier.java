package cz.muni.xmichalk.DocumentValidity;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.DTO.Token.Token;
import org.erdtman.jcs.JsonCanonicalizer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class StorageDocumentIntegrityVerifier {
    public static boolean verifySignature(Token token) {
        try {
            PublicKey publicKey = loadPublicKeyFromCertificate(token.data().additionalData().trustedPartyCertificate());

            ObjectMapper mapper = new ObjectMapper();
            String tokenDataJsonString = mapper.writeValueAsString(token.data());
            byte[] canonized = new JsonCanonicalizer(tokenDataJsonString).getEncodedUTF8();

            byte[] signatureBytes = Base64.getDecoder().decode(token.signature());

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(canonized);
            return verifier.verify(signatureBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey loadPublicKeyFromCertificate(String pemCert) throws Exception {
        ByteArrayInputStream certStream = new ByteArrayInputStream(pemCert.getBytes(StandardCharsets.UTF_8));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);
        return cert.getPublicKey();
    }

    public static boolean verifyHash(String data, String hashFunctionName, String expectedHexHash) {
        MessageDigest hashFunction = getHashFunction(hashFunctionName);
        byte[] hash = hashString(data, hashFunction);
        String hashHex = bytesToHex(hash);
        return hashHex.equals(expectedHexHash);
    }

    public static MessageDigest getHashFunction(String hashFunction) {
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
