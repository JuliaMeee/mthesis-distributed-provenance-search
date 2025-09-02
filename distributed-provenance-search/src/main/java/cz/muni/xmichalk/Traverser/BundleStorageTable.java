package cz.muni.xmichalk.Traverser;

import java.util.*;

public class BundleStorageTable {
    public static final Map<String, String> idToJsonldPath = Map.ofEntries(
            Map.entry("SamplingBundle_V0", "src/test/resources/data/SamplingBundle_V0.json"),
            Map.entry("SamplingBundle_V1", "src/test/resources/data/SamplingBundle_V1.json"),
            Map.entry("ProcessingBundle_V0", "src/test/resources/data/ProcessingBundle_V0.json"),
            Map.entry("ProcessingBundle_V1", "src/test/resources/data/ProcessingBundle_V1.json"),
            Map.entry("SpeciesIdentificationBundle_V0", "src/test/resources/data/SpeciesIdentificationBundle_V0.json"),
            Map.entry("DnaSequencingBundle_V0", "src/test/resources/data/DnaSequencingBundle_V0.json")


    );
}
