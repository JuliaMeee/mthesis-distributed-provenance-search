import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.targetSpecification.*;
import cz.muni.xmichalk.targetSpecification.attributeSpecification.QualifiedNameAttrSpecification;
import cz.muni.xmichalk.targetSpecification.logicalConnections.And;
import cz.muni.xmichalk.targetSpecification.logicalConnections.Implication;
import cz.muni.xmichalk.targetSpecification.logicalConnections.Or;
import cz.muni.xmichalk.targetSpecification.logicalConnections.Xor;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;
import static cz.muni.xmichalk.util.ProvDocumentUtils.deserializeFile;

public class TargetSpecificationTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";
    String specificationsFolder = System.getProperty("user.dir") + "/src/test/resources/targetSpecifications/";

    @Test
    public void testAllSatisfyNodeImplication() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification conWithMetaRef = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                List.of(ATTR_REFERENCED_META_BUNDLE_ID.getUri()),
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                ".*Connector"
                        )
                ));

        NodeSpecification nodeWithBundleRef = new NodeSpecification(
                null,
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                List.of(ATTR_REFERENCED_BUNDLE_ID.getUri()),
                null
        );

        Implication<INode> connectorImplication = new Implication<INode>(
                conWithMetaRef,
                nodeWithBundleRef
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new CountSpecification(new CountNodes(conWithMetaRef), EComparisonResult.GREATER_THAN_OR_EQUALS, 1),
                        new AllNodes(connectorImplication)
                ));

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testCountNonsenseNodes() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification nonsense = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                null
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(new CountSpecification(
                                new CountNodes(nonsense),
                                EComparisonResult.EQUALS,
                                0
                        )
                ));

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testHasSpecificMetaBundleId() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification mainActivityWithMetaRef = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                CPM_URI + "mainActivity"
                        ),
                        new QualifiedNameAttrSpecification(
                                ATTR_REFERENCED_META_BUNDLE_ID.getUri(),
                                ".*SamplingBundle_V0_meta"
                        )
                ));

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                new CountNodes(mainActivityWithMetaRef),
                                EComparisonResult.EQUALS,
                                1
                        )
                )
        );

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testCountSatisfiedAnd() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification conSpec = new NodeSpecification(
                "(?i).*spec",
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                ".*(forward|backward)Connector"
                        )
                ));

        NodeSpecification nodeWithBundleRefAndMetaRef = new NodeSpecification(
                null,
                null,
                null,
                List.of(ATTR_REFERENCED_BUNDLE_ID.getUri(), ATTR_REFERENCED_META_BUNDLE_ID.getUri()),
                null
        );

        And<INode> conjunction = new And<INode>(
                conSpec,
                nodeWithBundleRefAndMetaRef
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(new CountSpecification(
                                new CountNodes(conjunction),
                                EComparisonResult.EQUALS,
                                3
                        )
                ));

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testAllNodesImplicationOr() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V0.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        // Test that every connector is either a forward connector or has a referenced bundle id and meta bundle id defined

        NodeSpecification con = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                ".*commonprovenancemodel.*Connector"
                        )
                ));

        NodeSpecification nodeWithBundleRefAndMetaRef = new NodeSpecification(
                null,
                null,
                null,
                List.of(ATTR_REFERENCED_BUNDLE_ID.getUri(), ATTR_REFERENCED_META_BUNDLE_ID.getUri()),
                null
        );

        NodeSpecification forwardCon = new NodeSpecification(
                null,
                null,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                ".*forwardConnector"
                        )
                ));


        Or<INode> backwardOrForwardCon = new Or<INode>(
                nodeWithBundleRefAndMetaRef,
                forwardCon
        );

        Implication<INode> conImplication = new Implication<INode>(
                con,
                backwardOrForwardCon
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new AllNodes(conImplication)
                ));

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testXor() throws IOException {
        Path file = Path.of(dataFolder + "dataset2/ProcessingBundle_V0.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        AllNodes satisfied = new AllNodes(
                new NodeSpecification()
        );

        AllNodes unsatisfied = new AllNodes(
                new NodeSpecification(
                        null,
                        StatementOrBundle.Kind.PROV_ENTITY,
                        StatementOrBundle.Kind.PROV_ENTITY,
                        null,
                        null
                )
        );

        BundleSpecification bundleSpecificationSatisfied1 = new BundleSpecification(
                List.of(
                        new Xor<CpmDocument>(
                                satisfied,
                                unsatisfied
                        )
                ));

        BundleSpecification bundleSpecificationSatisfied2 = new BundleSpecification(
                List.of(
                        new Xor<CpmDocument>(
                                unsatisfied,
                                satisfied
                        )
                ));

        BundleSpecification bundleSpecificationUnsatisfied1 = new BundleSpecification(
                List.of(
                        new Xor<CpmDocument>(
                                unsatisfied,
                                unsatisfied
                        )
                ));

        BundleSpecification bundleSpecificationUnsatisfied2 = new BundleSpecification(
                List.of(
                        new Xor<CpmDocument>(
                                satisfied,
                                satisfied
                        )
                ));

        assert (bundleSpecificationSatisfied1.test(cpmDoc));
        assert (bundleSpecificationSatisfied2.test(cpmDoc));
        assert (!bundleSpecificationUnsatisfied1.test(cpmDoc));
        assert (!bundleSpecificationUnsatisfied2.test(cpmDoc));
    }

    @Test
    public void testCountNonsenseAnd() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification activity = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                null,
                null
        );

        NodeSpecification notActivity = new NodeSpecification(
                null,
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                null
        );

        And<INode> conjunction = new And<INode>(
                activity,
                notActivity
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(new CountSpecification(
                                new CountNodes(conjunction),
                                EComparisonResult.EQUALS,
                                0
                        )
                ));

        assert (bundleSpecification.test(cpmDoc));
    }

    @Test
    public void testHasForwardJumpConnectors() throws IOException {
        Path file = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Document document = deserializeFile(file, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        NodeSpecification forwardCon = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                CPM_URI + "forwardConnector"
                        )
                ));
        EdgeToNodeSpecification derivationEdgeToForwardCon = new EdgeToNodeSpecification(
                StatementOrBundle.Kind.PROV_DERIVATION,
                null,
                null,
                forwardCon
        );
        CountLinearSubgraphs forwardJumpChain = new CountLinearSubgraphs(
                forwardCon,
                List.of(derivationEdgeToForwardCon)
        );

        BundleSpecification bundleSpecification = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                forwardJumpChain,
                                EComparisonResult.GREATER_THAN_OR_EQUALS,
                                1
                        )
                )
        );

        assert (bundleSpecification.test(cpmDoc));
    }

    public BundleSpecification getSimpleValiditySpecification() {
        NodeSpecification mainActivityNode = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                CPM_URI + "mainActivity"
                        )
                ));

        NodeSpecification activityWithMetaRef = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ACTIVITY,
                null,
                List.of(
                        ATTR_REFERENCED_META_BUNDLE_ID.getUri()
                ),
                null);

        NodeSpecification backwardCon = new NodeSpecification(
                null,
                StatementOrBundle.Kind.PROV_ENTITY,
                null,
                null,
                List.of(
                        new QualifiedNameAttrSpecification(
                                ATTR_PROV_TYPE.getUri(),
                                CPM_URI + "backwardConnector"
                        )
                ));

        NodeSpecification hasBackwardConAttributes = new NodeSpecification(
                null,
                null,
                null,
                List.of(
                        ATTR_REFERENCED_META_BUNDLE_ID.getUri(),
                        ATTR_REFERENCED_BUNDLE_ID.getUri(),
                        ATTR_REFERENCED_BUNDLE_HASH_VALUE.getUri(),
                        ATTR_HASH_ALG.getUri()
                ),
                null);


        return new BundleSpecification(
                List.of(
                        // Has exactly one main activity
                        new CountSpecification(
                                new CountNodes(mainActivityNode),
                                EComparisonResult.EQUALS,
                                1
                        ),
                        // Main activity has ref to meta bundle
                        new AllNodes(
                                new Implication<INode>(
                                        mainActivityNode,
                                        activityWithMetaRef
                                )),
                        // All backward connectors have necessary attributes specified
                        new AllNodes(
                                new Implication<>(
                                        backwardCon,
                                        hasBackwardConAttributes
                                )
                        )
                )
        );
    }

    @Test
    public void testSimpleValiditySpecification() throws IOException {
        BundleSpecification validitySpecs = getSimpleValiditySpecification();

        for (int i : List.of(1, 2, 3, 4)) {
            String datasetFolder = dataFolder + "dataset" + i + "/";
            try (var paths = Files.walk(Paths.get(datasetFolder))) {
                paths.filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            Document document = null;
                            try {
                                document = deserializeFile(filePath, Formats.ProvFormat.JSON);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);
                            assert (validitySpecs.test(cpmDoc));
                        });
            }
        }
    }

    @Test
    public void testFromFiles() throws IOException {
        Path docFile = Path.of(dataFolder + "dataset1/SamplingBundle_V1.json");
        Path specsFile = Path.of(specificationsFolder + "connectorRules.json");
        testFromFile(docFile, specsFile);

        docFile = Path.of(dataFolder + "dataset2/ProcessingBundle_V0.json");
        specsFile = Path.of(specificationsFolder + "has1MainActivity.json");
        testFromFile(docFile, specsFile);

        docFile = Path.of(dataFolder + "dataset3/SpeciesIdentificationBundle_V0.json");
        specsFile = Path.of(specificationsFolder + "hasBackwardJumpConnectors.json");
        testFromFile(docFile, specsFile);
    }


    public void testFromFile(Path docPath, Path specsPath) throws IOException {
        Document document = deserializeFile(docPath, Formats.ProvFormat.JSON);
        CpmDocument cpmDoc = new CpmDocument(document, pF, cPF, cF);

        ObjectMapper mapper = new ObjectMapper();

        ITestableSpecification<CpmDocument> specification =
                mapper.readValue(
                        specsPath.toFile(),
                        new TypeReference<ITestableSpecification<CpmDocument>>() {
                        }
                );
        assert (specification.test(cpmDoc));

    }

    @Test
    public void ttt() {
        var samplingMainActivity = new NodeSpecification();
        samplingMainActivity.idUriRegex = "(?i).*Sampling.*";
        samplingMainActivity.isKind = StatementOrBundle.Kind.PROV_ACTIVITY;
        samplingMainActivity.hasAttributeValues = List.of(
                new QualifiedNameAttrSpecification(
                        ATTR_PROV_TYPE.getUri(),
                        CPM_URI + "mainActivity"
                )
        );

        var bundleSpec = new BundleSpecification(
                List.of(
                        new CountSpecification(
                                new CountNodes(samplingMainActivity),
                                EComparisonResult.EQUALS,
                                1
                        )
                )
        );

        var objectMapper = new ObjectMapper();
        var x = objectMapper.valueToTree(bundleSpec);
        var str = x.toString();
    }
}
