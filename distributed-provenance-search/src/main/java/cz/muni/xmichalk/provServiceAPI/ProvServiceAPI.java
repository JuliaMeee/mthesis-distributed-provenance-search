package cz.muni.xmichalk.provServiceAPI;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.dto.BundleQueryDTO;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.dto.PickVersionParamsDTO;
import cz.muni.xmichalk.dto.QualifiedNameDTO;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class ProvServiceAPI {
    private static final Logger log = LoggerFactory.getLogger(ProvServiceAPI.class);

    public static QualifiedName fetchPreferredBundleVersion(String serviceUri, QualifiedName bundleId, String versionPreference) throws IOException {
        PickVersionParamsDTO params = new PickVersionParamsDTO(
                new QualifiedNameDTO().from(bundleId),
                versionPreference);

        if (serviceUri == null) {
            throw new IllegalArgumentException("Prov service cannot be null.");
        }

        String url = serviceUri + "pickVersion";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PickVersionParamsDTO> request = new HttpEntity<>(params, headers);

        ResponseEntity<QualifiedNameDTO> response = restTemplate.postForEntity(
                url, request, QualifiedNameDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Get preferred version API call failed with status: " + response.getStatusCode());
        }

        if (response.getBody() == null) {
            return null;
        }

        return response.getBody().toQN();
    }

    public static BundleQueryResultDTO fetchBundleQueryResult(String serviceUri,
                                                              QualifiedName bundleId, QualifiedName connectorId,
                                                              String queryType, JsonNode querySpecification) throws IOException {
        BundleQueryDTO queryParams = new BundleQueryDTO(bundleId, connectorId, queryType, querySpecification);

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
}
