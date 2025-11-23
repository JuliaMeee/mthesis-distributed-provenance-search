package cz.muni.xmichalk.provServiceAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.dto.BundleQueryDTO;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.dto.QualifiedNameDTO;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class ProvServiceAPI {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static BundleQueryResultDTO fetchBundleQueryResult(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, JsonNode querySpecification) throws IOException {
        BundleQueryDTO queryParams = new BundleQueryDTO(bundleId, connectorId, querySpecification);

        if (serviceUri == null) {
            throw new IllegalArgumentException("Prov service cannot be null.");
        }

        String url = serviceUri + "bundleQuery";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BundleQueryDTO> request = new HttpEntity<>(queryParams, headers);

        ResponseEntity<BundleQueryResultDTO> response = restTemplate.postForEntity(
                url, request, BundleQueryResultDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Bundle query API call failed with status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public static QualifiedName fetchPreferredBundleVersion(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, String versionPreference) throws IOException {
        BundleQueryResultDTO queryResult = fetchBundleQueryResult(
                serviceUri, bundleId, connectorId,
                objectMapper.readTree("""
                            {
                              "type": "GetPreferredVersion",
                              "versionPreference": "%s"
                            }
                        """.formatted(versionPreference)
                )
        );

        if (queryResult == null || queryResult.result == null) {
            return null;
        }

        QualifiedNameDTO pickedBundleIdDto = objectMapper.convertValue(queryResult.result, new TypeReference<QualifiedNameDTO>() {
        });

        return pickedBundleIdDto.toQN();
    }

    public static BundleQueryResultDTO fetchBundleConnectors(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, boolean backward) throws IOException {

        return fetchBundleQueryResult(
                serviceUri, bundleId, connectorId,
                objectMapper.readTree("""
                            {
                              "type": "GetConnectors",
                              "backward": %s
                            }
                        """.formatted(backward)
                )
        );
    }
}
