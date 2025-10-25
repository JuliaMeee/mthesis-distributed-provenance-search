package cz.muni.xmichalk.Models;

import com.fasterxml.jackson.databind.JsonNode;
import org.openprovenance.prov.model.QualifiedName;

public class FoundResult {
    public QualifiedName bundleId;
    public JsonNode result;
    public boolean hasPathIntegrity;
    public boolean hasIntegrity;
    public boolean isPathValid;
    public boolean isValid;


    public FoundResult() {

    }

    public FoundResult(QualifiedName bundleId, JsonNode result, boolean hasPathIntegrity, boolean hasIntegrity, boolean isPathValid, boolean isValid) {
        this.bundleId = bundleId;
        this.result = result;
        this.hasPathIntegrity = hasPathIntegrity;
        this.hasIntegrity = hasIntegrity;
        this.isPathValid = isPathValid;
        this.isValid = isValid;
    }
}
