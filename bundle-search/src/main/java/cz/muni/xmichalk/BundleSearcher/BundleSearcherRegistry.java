package cz.muni.xmichalk.BundleSearcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class BundleSearcherRegistry {
    private static final Map<String, Function<String, IBundleSearcher>> registry = new HashMap<>();
    // Maps target type to searcher constructor function that takes a target specification as input

    static {
        registry.put("demo_node_localname", (String targetSpecification) ->
                new BFSBundleNodeSearcher(node -> Objects.equals(node.getId().getLocalPart(), targetSpecification))
        );
        // Add more target types here in the future development (eg. subgraph_match_json, etc.)
    }

    public static Function<String, IBundleSearcher> getSearcherConstructor(String id) {
        return registry.get(id);

    }

    public static List<String> GetAllTargetTypes() {
        return registry.keySet().stream().toList();
    }
}
