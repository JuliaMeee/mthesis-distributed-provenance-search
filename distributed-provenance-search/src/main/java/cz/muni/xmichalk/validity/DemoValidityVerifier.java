package cz.muni.xmichalk.validity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.models.ItemToTraverse;
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
    public boolean verify(ItemToTraverse itemToTraverse, BundleQueryResultDTO queryResult) {
        String bundleUri = queryResult.token.data().additionalData().bundle();

        String provServiceUri = provServiceTable.getServiceUri(bundleUri);

        BundleQueryResultDTO result = null;
        try {
            result = ProvServiceAPI.fetchBundleQueryResult(provServiceUri, itemToTraverse.bundleId, itemToTraverse.connectorId, validitySpecification);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (result == null) {
            throw new RuntimeException("Fetch TEST_FITS result for bundle: " + itemToTraverse.bundleId.getUri() + " returned null.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(result.result, Boolean.class);
    }
}
