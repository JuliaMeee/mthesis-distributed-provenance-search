import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.targetSpecification.ICondition;
import cz.muni.xmichalk.targetSpecification.bundleConditions.AllNodes;
import cz.muni.xmichalk.targetSpecification.bundleConditions.CountCondition;
import cz.muni.xmichalk.targetSpecification.bundleConditions.EComparisonResult;
import cz.muni.xmichalk.targetSpecification.findable.FindLinearSubgraphs;
import cz.muni.xmichalk.targetSpecification.findable.FindNodes;
import cz.muni.xmichalk.targetSpecification.logicalOperations.AllTrue;
import cz.muni.xmichalk.targetSpecification.logicalOperations.AnyTrue;
import cz.muni.xmichalk.targetSpecification.logicalOperations.Either;
import cz.muni.xmichalk.targetSpecification.logicalOperations.Implication;
import cz.muni.xmichalk.targetSpecification.nodeConditions.*;
import cz.muni.xmichalk.targetSpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.targetSpecification.subgraphConditions.edgeConditions.IsNotRelation;
import cz.muni.xmichalk.targetSpecification.subgraphConditions.edgeConditions.IsRelation;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.StatementOrBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;
import static cz.muni.xmichalk.util.NameSpaceConstants.SCHEMA_URI;

public class TargetSpecificationTest {

    String specificationsFolder = System.getProperty("user.dir") + "/src/test/resources/targetSpecifications/";
    CpmDocument samplingBundle_V0 = TestDocument.getSamplingBundle_V0();
    CpmDocument samplingBundle_V1 = TestDocument.getSamplingBundle_V1();
    CpmDocument processingBundle_V0 = TestDocument.getProcessingBundle_V0();
    CpmDocument processingBundle_V1 = TestDocument.getProcessingBundle_V1();
    CpmDocument speciesIdentificationBundle_V0 = TestDocument.getSpeciesIdentificationBundle_V0();
    CpmDocument dnaSequencingBundle_V0 = TestDocument.getDnaSequencingBundle_V0();

    public TargetSpecificationTest() throws IOException {
    }

    ICondition<INode> isMainActivity = new HasAttrQualifiedNameValue(
            ATTR_PROV_TYPE.getUri(),
            CPM_URI + "mainActivity"
    );

    ICondition<INode> isConnector = new HasAttrQualifiedNameValue(
            ATTR_PROV_TYPE.getUri(),
            CPM_URI + "(backward|forward)Connector"
    );

    ICondition<INode> isBackwardConn = new HasAttrQualifiedNameValue(
            ATTR_PROV_TYPE.getUri(),
            CPM_URI + "backwardConnector"
    );

    ICondition<INode> isForwardConn = new HasAttrQualifiedNameValue(
            ATTR_PROV_TYPE.getUri(),
            CPM_URI + "forwardConnector"
    );

    ICondition<INode> nonsenseNodeCondition = new AllTrue<INode>(List.of(
            new IsKind(StatementOrBundle.Kind.PROV_ACTIVITY),
            new IsNotKind(StatementOrBundle.Kind.PROV_ACTIVITY)
    ));

    ICondition<INode> isActivityWithMetaRef = new AllTrue<>(List.of(
            new IsKind(StatementOrBundle.Kind.PROV_ACTIVITY),
            new HasAttr(ATTR_REFERENCED_META_BUNDLE_ID.getUri())
    ));

    ICondition<INode> hasBundleRefAndMetaRef = new AllTrue<>(List.of(
            new HasAttr(ATTR_REFERENCED_BUNDLE_ID.getUri()),
            new HasAttr(ATTR_REFERENCED_META_BUNDLE_ID.getUri())
    ));

    ICondition<INode> hasBackwardConAttributes = new AllTrue<>(List.of(
            new HasAttr(ATTR_REFERENCED_META_BUNDLE_ID.getUri()),
            new HasAttr(ATTR_REFERENCED_BUNDLE_ID.getUri()),
            new HasAttr(ATTR_REFERENCED_BUNDLE_HASH_VALUE.getUri()),
            new HasAttr(ATTR_HASH_ALG.getUri())
    ));

    ICondition<CpmDocument> simpleValidityCondition = new AllTrue<CpmDocument>(
            List.of(
                    // Has exactly one main activity
                    new CountCondition(
                            new FindNodes(isMainActivity),
                            EComparisonResult.EQUALS,
                            1
                    ),
                    // Main activity has ref to meta bundle
                    new AllNodes(
                            new Implication<INode>(
                                    isMainActivity,
                                    isActivityWithMetaRef
                            )),
                    // All backward connectors have necessary attributes specified
                    new AllNodes(
                            new Implication<>(
                                    isBackwardConn,
                                    hasBackwardConAttributes
                            )
                    )
            )
    );

    ICondition<INode> isPerson = new HasAttrQualifiedNameValue(
            ATTR_PROV_TYPE.getUri(),
            SCHEMA_URI + "Person");


    @Test
    public void testAllSatisfyNodeImplication() throws IOException {
        ICondition<INode> conWithRefs = new AllTrue<INode>(List.of(
                isConnector,
                hasBundleRefAndMetaRef
        ));

        ICondition<INode> nonActivityWithBundleRef = new AllTrue<INode>(List.of(
                new IsNotKind(StatementOrBundle.Kind.PROV_ACTIVITY),
                new HasAttr(ATTR_REFERENCED_BUNDLE_ID.getUri())
        ));

        Implication<INode> connectorImplication = new Implication<INode>(
                conWithRefs,
                nonActivityWithBundleRef
        );

        ICondition<CpmDocument> bundleSpecification = new AllTrue<CpmDocument>(List.of(
                new CountCondition(new FindNodes(conWithRefs), EComparisonResult.GREATER_THAN_OR_EQUALS, 1),
                new AllNodes(connectorImplication)
        ));

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    public void testCountNonsenseNodes() throws IOException {
        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(nonsenseNodeCondition),
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    public void testHasSpecificMetaBundleId() throws IOException {
        ICondition<INode> mainActivityWithMetaRef = new AllTrue<>(List.of(
                isMainActivity,
                isActivityWithMetaRef
        ));

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(mainActivityWithMetaRef),
                EComparisonResult.EQUALS,
                1
        );

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    public void testCountSatisfiedAllTrue() throws IOException {
        ICondition<INode> conSpec = new AllTrue<>(List.of(
                new HasId("(?i).*spec"),
                isConnector
        ));

        AllTrue<INode> joinedConditions = new AllTrue<INode>(List.of(
                conSpec,
                hasBundleRefAndMetaRef
        ));

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(joinedConditions),
                EComparisonResult.EQUALS,
                3
        );

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    public void testAllNodesImplicationAnyTrue() throws IOException {
        // Test that every connector is either a forward connector or has a referenced bundle id and meta bundle id defined

        AnyTrue<INode> backwardOrForwardCon = new AnyTrue<INode>(List.of(
                hasBundleRefAndMetaRef,
                isForwardConn)
        );

        Implication<INode> conImplication = new Implication<INode>(
                isConnector,
                backwardOrForwardCon
        );

        ICondition<CpmDocument> bundleSpecification = new AllNodes(conImplication);

        assert (bundleSpecification.test(processingBundle_V0));
    }

    @Test
    public void testEither() throws IOException {
        AllNodes satisfied = new AllNodes(
                new HasId(".*")
        );

        AllNodes unsatisfied = new AllNodes(
                nonsenseNodeCondition
        );

        ICondition<CpmDocument> bundleSpecificationSatisfied1 = new Either<CpmDocument>(
                satisfied,
                unsatisfied
        );

        ICondition<CpmDocument> bundleSpecificationSatisfied2 = new Either<CpmDocument>(
                unsatisfied,
                satisfied
        );

        ICondition<CpmDocument> bundleSpecificationUnsatisfied1 = new Either<CpmDocument>(
                unsatisfied,
                unsatisfied
        );

        ICondition<CpmDocument> bundleSpecificationUnsatisfied2 = new Either<CpmDocument>(
                satisfied,
                satisfied
        );

        assert (bundleSpecificationSatisfied1.test(processingBundle_V0));
        assert (bundleSpecificationSatisfied2.test(processingBundle_V0));
        assert (!bundleSpecificationUnsatisfied1.test(processingBundle_V0));
        assert (!bundleSpecificationUnsatisfied2.test(processingBundle_V0));
    }

    @Test
    public void testCountNonsenseConjunction() throws IOException {
        ICondition<INode> activity = new IsKind(StatementOrBundle.Kind.PROV_ACTIVITY);

        ICondition<INode> notActivity = new IsNotKind(StatementOrBundle.Kind.PROV_ACTIVITY);

        AllTrue<INode> conjunction = new AllTrue<INode>(List.of(
                activity,
                notActivity
        ));

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                new FindNodes(conjunction),
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    void testIsRelation() {
        ICondition<IEdge> isDerivation = new IsRelation(StatementOrBundle.Kind.PROV_DERIVATION);

        assert (samplingBundle_V1.getEdges().stream().anyMatch(edge -> isDerivation.test(edge)));
    }

    @Test
    void testIsNotRelation() {
        ICondition<IEdge> isNotDerivation = new IsNotRelation(StatementOrBundle.Kind.PROV_DERIVATION);

        assert (samplingBundle_V1.getEdges().stream().anyMatch(edge -> isNotDerivation.test(edge)));
    }

    @Test
    public void testHasForwardJumpConnectors() throws IOException {
        FindLinearSubgraphs forwardJumpChain = new FindLinearSubgraphs(
                List.of(
                        new EdgeToNodeCondition(
                                null,
                                isForwardConn,
                                null
                        ),
                        new EdgeToNodeCondition(
                                new IsRelation(StatementOrBundle.Kind.PROV_DERIVATION),
                                isForwardConn,
                                true
                        )
                )
        );

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                forwardJumpChain,
                EComparisonResult.GREATER_THAN_OR_EQUALS,
                1
        );

        assert (bundleSpecification.test(samplingBundle_V1));
    }

    @Test
    public void hasBackwardJumpConnectorTo() throws IOException {
        FindLinearSubgraphs backwardJumpChain = new FindLinearSubgraphs(
                List.of(
                        new EdgeToNodeCondition(
                                null,
                                new AllTrue<>(List.of(
                                        isBackwardConn,
                                        new HasAttrQualifiedNameValue(
                                                ATTR_REFERENCED_META_BUNDLE_ID.getUri(),
                                                "http://prov-storage-1:8000/api/v1/documents/meta/SamplingBundle_V0_meta"
                                        )
                                )),
                                null
                        ),
                        new EdgeToNodeCondition(
                                new IsRelation(StatementOrBundle.Kind.PROV_DERIVATION),
                                isBackwardConn,
                                true
                        )
                )
        );

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                backwardJumpChain,
                EComparisonResult.GREATER_THAN_OR_EQUALS,
                1
        );

        assert (bundleSpecification.test(speciesIdentificationBundle_V0));
    }

    @Test
    public void testDoesNotHaveForwardJumpConnectors() throws IOException {
        FindLinearSubgraphs forwardJumpChain = new FindLinearSubgraphs(
                List.of(
                        new EdgeToNodeCondition(
                                null,
                                isForwardConn,
                                null
                        ),
                        new EdgeToNodeCondition(
                                new IsRelation(StatementOrBundle.Kind.PROV_DERIVATION),
                                isForwardConn,
                                null
                        )
                )
        );

        ICondition<CpmDocument> bundleSpecification = new CountCondition(
                forwardJumpChain,
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(processingBundle_V1));
    }


    @Test
    public void testSimpleValiditySpecification() throws IOException {
        List<CpmDocument> cpmDocs = List.of(
                samplingBundle_V0,
                samplingBundle_V1,
                processingBundle_V0,
                processingBundle_V1,
                speciesIdentificationBundle_V0,
                dnaSequencingBundle_V0
        );

        for (CpmDocument cpmDoc : cpmDocs) {
            assert (simpleValidityCondition.test(cpmDoc));
        }
    }

    @Test
    public void testSerializationRoundTrip() throws IOException {
        CpmDocument cpmDoc = samplingBundle_V1;

        assert (simpleValidityCondition.test(cpmDoc));

        Path filePath = Path.of(specificationsFolder, "simpleValidityCondition.json");

        serializeToJson(filePath, simpleValidityCondition);

        ObjectMapper mapper = new ObjectMapper();
        ICondition<CpmDocument> deserialized =
                mapper.readValue(
                        filePath.toFile(),
                        new TypeReference<ICondition<CpmDocument>>() {
                        }
                );
        assert (deserialized.test(cpmDoc));

    }

    public void serializeToJson(Path filePath, Object object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        Files.writeString(filePath, json);
    }
}
