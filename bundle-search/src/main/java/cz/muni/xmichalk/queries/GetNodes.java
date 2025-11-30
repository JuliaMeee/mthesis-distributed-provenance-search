package cz.muni.xmichalk.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;
import cz.muni.xmichalk.util.ProvDocumentUtils;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;
import java.util.List;

public class GetNodes implements IQuery<JsonNode> {
    public IFindableInDocument<INode> nodeFinder;

    public GetNodes() {
    }

    public GetNodes(IFindableInDocument<INode> nodeFinder) {
        this.nodeFinder = nodeFinder;
    }

    @Override
    public JsonNode evaluate(BundleStart input) {
        if (nodeFinder == null) {
            return null;
        }
        List<INode> foundNodes = nodeFinder.find(input.bundle, input.startNode);

        return transformNodesToDocJson(foundNodes);
    }

    private JsonNode transformNodesToDocJson(List<INode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        Document resultsDocument = ProvDocumentUtils.encapsulateInDocument(nodes, null);
        String jsonString = ProvDocumentUtils.serialize(resultsDocument, Formats.ProvFormat.JSON);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert results document to JSON", e);
        }
    }
}
