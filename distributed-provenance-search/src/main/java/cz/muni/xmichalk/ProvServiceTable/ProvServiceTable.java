package cz.muni.xmichalk.ProvServiceTable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProvServiceTable implements IProvServiceTable {
    private final LinkedHashMap<String, String> table = new LinkedHashMap<>();

    public String getServiceUri(String resourceUri) {
        for (Map.Entry<String, String> entry : table.entrySet()) {
            if (resourceUri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void loadFromJson(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        LinkedHashMap<String, String> loaded =
                mapper.readValue(input,
                        mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
        table.putAll(loaded);

        System.out.println("Loaded traverser table: " + table);
    }
}