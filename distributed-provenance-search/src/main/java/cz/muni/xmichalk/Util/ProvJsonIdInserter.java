package cz.muni.xmichalk.Util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;

public class ProvJsonIdInserter {
    public static void insertIds(String inputFilePath, String outputFilePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File inputFile = new File(inputFilePath);
        ObjectNode root = (ObjectNode) mapper.readTree(inputFile);

        ObjectNode bundle = (ObjectNode) root.path("bundle").elements().next();
        for (String section : new String[]{"entity", "activity", "agent"}) {
            if (bundle.has(section)) {
                ObjectNode sectionNode = (ObjectNode) bundle.get(section);
                sectionNode.fieldNames().forEachRemaining(key -> {
                    ObjectNode obj = (ObjectNode) sectionNode.get(key);
                    if (!obj.has("@id")) {
                        obj.put("@id", key);
                    }
                });
            }
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(outputFilePath), root);
    }
}