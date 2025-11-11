package cz.muni.xmichalk.ProvServiceAPI;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.DTO.BundleSearchParamsDTO;
import cz.muni.xmichalk.DTO.BundleSearchResultDTO;
import cz.muni.xmichalk.DTO.PickVersionParamsDTO;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
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

        return response.getBody().toDomainModel();
    }

    public static BundleSearchResultDTO fetchSearchBundleResult(String serviceUri,
                                                                QualifiedName bundleId, QualifiedName connectorId,
                                                                String targetType, JsonNode targetSpecification) throws IOException {
        BundleSearchParamsDTO searchParams = new BundleSearchParamsDTO(bundleId, connectorId, targetType, targetSpecification);

        if (serviceUri == null) {
            throw new IllegalArgumentException("Prov service cannot be null.");
        }

        String url = serviceUri + "searchBundle";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BundleSearchParamsDTO> request = new HttpEntity<>(searchParams, headers);

        ResponseEntity<BundleSearchResultDTO> response = restTemplate.postForEntity(
                url, request, BundleSearchResultDTO.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Search bundle API call failed with status: " + response.getStatusCode());
        }

        BundleSearchResultDTO searchBundleResultDTO = response.getBody();

        return searchBundleResultDTO;
    }
}
