package cz.muni.xmichalk.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openprovenance.prov.model.interop.Formats;

import java.io.IOException;

public class ProvJsonUtils {
    /**
     * Add explicit "@id" property to bundle to comply with provtoolbox deserialization requirements.
     *
     * @param json the original JSON string possibly without "@id" in bundle
     * @return the modified JSON string in minimized format with proper "@id" added to bundle
     */
    public static String addExplicitBundleId(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(json);

            var bundleNode = root.path("bundle");
            if (bundleNode.isObject()) {
                var fieldNames = bundleNode.fieldNames();
                if (fieldNames.hasNext()) {
                    String bundleId = fieldNames.next();
                    var bundleObj = (ObjectNode) bundleNode.path(bundleId);
                    if (!bundleObj.has("@id")) {
                        bundleObj.put("@id", bundleId);
                    }
                }
            }
            return mapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to add explicit bundle id", e);
        }
    }

    public static String removeExplicitBundleId(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(json);

            var bundleNode = root.path("bundle");
            if (bundleNode.isObject()) {
                var fieldNames = bundleNode.fieldNames();
                if (fieldNames.hasNext()) {
                    String bundleId = fieldNames.next();
                    var bundleObj = (ObjectNode) bundleNode.path(bundleId);
                    
                    if (bundleObj.has("@id")) {
                        bundleObj.remove("@id");
                    }
                }
            }

            return mapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove explicit bundle id", e);
        }
    }

    public static String stringifyValues(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode stringified = stringifyNode(root, mapper);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stringified);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stringify values", e);
        }
    }

    private static JsonNode stringifyNode(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            ObjectNode obj = mapper.createObjectNode();
            node.fieldNames().forEachRemaining(field -> {
                obj.set(field, stringifyNode(node.get(field), mapper));
            });
            return obj;
        } else if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            node.forEach(item -> arr.add(stringifyNode(item, mapper)));
            return arr;
        } else {
            return new TextNode(node.asText());
        }
    }

    public static String copyOuterPrefixesIntoBundles(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode outerPrefix = root.path("prefix");
            JsonNode bundleNode = root.path("bundle");
            if (outerPrefix.isObject() && bundleNode.isObject()) {
                for (var it = bundleNode.fieldNames(); it.hasNext(); ) {
                    String bundleId = it.next();
                    ObjectNode bundleObj = (ObjectNode) bundleNode.path(bundleId);
                    
                    ObjectNode bundlePrefix = bundleObj.has("prefix") && bundleObj.get("prefix").isObject()
                            ? (ObjectNode) bundleObj.get("prefix")
                            : mapper.createObjectNode();
                    
                    outerPrefix.fields().forEachRemaining(entry -> {
                        bundlePrefix.set(entry.getKey(), entry.getValue());
                    });

                    bundleObj.set("prefix", bundlePrefix);
                }
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy outer prefixes into bundles", e);
        }
    }
}