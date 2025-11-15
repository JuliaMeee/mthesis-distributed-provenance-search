package cz.muni.xmichalk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;

public class ProvJsonUtils {


    public static String prepareJsonForDeserialization(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(json);

            root = addExplicitBundleId(root);
            root = stringifyValues(root, mapper);
            root = copyOuterPrefixesIntoBundles(root, mapper);
            root = reformatProvType(root, mapper);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare JSON for deserialization", e);
        }
    }

    /**
     * Add explicit "@id" property to bundle to comply with provtoolbox deserialization requirements.
     *
     * @param root the original JSON, possibly without "@id" in bundle
     * @return the modified JSON string in minimized format with proper "@id" added to bundle
     */
    public static JsonNode addExplicitBundleId(JsonNode root) {
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
        return root;
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

    private static JsonNode stringifyValues(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            ObjectNode obj = mapper.createObjectNode();
            node.fieldNames().forEachRemaining(field -> {
                obj.set(field, stringifyValues(node.get(field), mapper));
            });
            return obj;
        } else if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            node.forEach(item -> arr.add(stringifyValues(item, mapper)));
            return arr;
        } else {
            return new TextNode(node.asText());
        }
    }

    public static JsonNode copyOuterPrefixesIntoBundles(JsonNode root, ObjectMapper mapper) {
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

        return root;
    }

    private static JsonNode reformatProvType(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fields().forEachRemaining(entry -> {
                if ("prov:type".equals(entry.getKey()) && entry.getValue().isTextual()) {
                    String value = entry.getValue().asText();
                    ArrayNode arr = mapper.createArrayNode();
                    ObjectNode typeObj = mapper.createObjectNode();
                    typeObj.put("type", "prov:QUALIFIED_NAME");
                    typeObj.put("$", value);
                    arr.add(typeObj);
                    obj.set("prov:type", arr);
                } else {
                    reformatProvType(entry.getValue(), mapper);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                reformatProvType(item, mapper);
            }
        }

        return node;
    }
}