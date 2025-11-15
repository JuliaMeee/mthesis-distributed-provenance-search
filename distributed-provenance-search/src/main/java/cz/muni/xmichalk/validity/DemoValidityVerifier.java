package cz.muni.xmichalk.validity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.dto.BundleSearchResultDTO;
import cz.muni.xmichalk.models.ItemToSearch;
import cz.muni.xmichalk.provServiceAPI.ProvServiceAPI;
import cz.muni.xmichalk.provServiceTable.IProvServiceTable;

import java.io.IOException;
import java.io.InputStream;

public class DemoValidityVerifier implements IValidityVerifier {
    private final IProvServiceTable provServiceTable;
    private final JsonNode validitySpecification;

    public DemoValidityVerifier(IProvServiceTable provServiceTable, InputStream input) {
        this.provServiceTable = provServiceTable;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            validitySpecification = objectMapper.readTree(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public boolean verify(ItemToSearch itemToSearch, BundleSearchResultDTO bundleSearchResult) {
        String bundleUri = bundleSearchResult.token().data().additionalData().bundle();

        String provServiceUri = provServiceTable.getServiceUri(bundleUri);

        BundleSearchResultDTO result = null;
        try {
            result = ProvServiceAPI.fetchSearchBundleResult(provServiceUri, itemToSearch.bundleId, itemToSearch.connectorId, "TEST_FITS", validitySpecification);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (result == null) {
            throw new RuntimeException("Fetch test bundle result for bundle: " + itemToSearch.bundleId.getUri() + " returned null.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(result.found(), Boolean.class);
    }
}
