package cz.muni.xmichalk.provServiceAPI;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import org.openprovenance.prov.model.QualifiedName;

public interface IProvServiceAPI {
    BundleQueryResultDTO fetchBundleQueryResult(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, JsonNode querySpecification);

    QualifiedName fetchPreferredBundleVersion(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, String versionPreference);

    BundleQueryResultDTO fetchBundleConnectors(
            String serviceUri, QualifiedName bundleId, QualifiedName connectorId, boolean backward);
}
