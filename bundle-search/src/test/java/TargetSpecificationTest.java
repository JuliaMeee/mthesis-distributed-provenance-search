import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.querySpecification.bundleConditions.AllNodes;
import cz.muni.xmichalk.querySpecification.bundleConditions.CountCondition;
import cz.muni.xmichalk.querySpecification.bundleConditions.EComparisonResult;
import cz.muni.xmichalk.querySpecification.findable.FindFittingLinearSubgraphs;
import cz.muni.xmichalk.querySpecification.findable.FindFittingNodes;
import cz.muni.xmichalk.querySpecification.findable.IFindableInDocument;
import cz.muni.xmichalk.querySpecification.logicalOperations.AllTrue;
import cz.muni.xmichalk.querySpecification.logicalOperations.AnyTrue;
import cz.muni.xmichalk.querySpecification.logicalOperations.Either;
import cz.muni.xmichalk.querySpecification.logicalOperations.Implication;
import cz.muni.xmichalk.querySpecification.nodeConditions.*;
import cz.muni.xmichalk.querySpecification.subgraphConditions.EdgeToNodeCondition;
import cz.muni.xmichalk.querySpecification.subgraphConditions.edgeConditions.IsNotRelation;
import cz.muni.xmichalk.querySpecification.subgraphConditions.edgeConditions.IsRelation;
import cz.muni.xmichalk.util.CpmUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.vanilla.QualifiedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.*;

public class TargetSpecificationTest {
    String specificationsFolder = System.getProperty("user.dir") + "/src/test/resources/targetSpecifications/";

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

    ICondition<BundleStart> simpleValidityCondition = new AllTrue<BundleStart>(
            List.of(
                    // Has exactly one main activity
                    new CountCondition(
                            new FindFittingNodes(isMainActivity),
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
    public void testAllSatisfyNodeImplication() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

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

        ICondition<BundleStart> bundleSpecification = new AllTrue<BundleStart>(List.of(
                new CountCondition(new FindFittingNodes(conWithRefs), EComparisonResult.GREATER_THAN_OR_EQUALS, 1),
                new AllNodes(connectorImplication)
        ));

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testCountNonsenseNodes() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(nonsenseNodeCondition),
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testHasSpecificMetaBundleId() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<INode> mainActivityWithMetaRef = new AllTrue<>(List.of(
                isMainActivity,
                isActivityWithMetaRef
        ));

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(mainActivityWithMetaRef),
                EComparisonResult.EQUALS,
                1
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testCountSatisfiedAllTrue() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<INode> conSpec = new AllTrue<>(List.of(
                new HasId("(?i).*spec"),
                isConnector
        ));

        AllTrue<INode> joinedConditions = new AllTrue<INode>(List.of(
                conSpec,
                hasBundleRefAndMetaRef
        ));

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(joinedConditions),
                EComparisonResult.EQUALS,
                3
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testAllNodesImplicationAnyTrue() {
        // Test that every connector is either a forward connector or has a referenced bundle id and meta bundle id defined
        CpmDocument cpmDoc = TestDocumentProvider.processingBundle_V0;

        AnyTrue<INode> backwardOrForwardCon = new AnyTrue<INode>(List.of(
                hasBundleRefAndMetaRef,
                isForwardConn)
        );

        Implication<INode> conImplication = new Implication<INode>(
                isConnector,
                backwardOrForwardCon
        );

        ICondition<BundleStart> bundleSpecification = new AllNodes(conImplication);

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testEither() {
        CpmDocument cpmDoc = TestDocumentProvider.processingBundle_V0;

        AllNodes satisfied = new AllNodes(
                new HasId(".*")
        );

        AllNodes unsatisfied = new AllNodes(
                nonsenseNodeCondition
        );

        ICondition<BundleStart> bundleSpecificationSatisfied1 = new Either<BundleStart>(
                satisfied,
                unsatisfied
        );

        ICondition<BundleStart> bundleSpecificationSatisfied2 = new Either<BundleStart>(
                unsatisfied,
                satisfied
        );

        ICondition<BundleStart> bundleSpecificationUnsatisfied1 = new Either<BundleStart>(
                unsatisfied,
                unsatisfied
        );

        ICondition<BundleStart> bundleSpecificationUnsatisfied2 = new Either<BundleStart>(
                satisfied,
                satisfied
        );

        assert (bundleSpecificationSatisfied1.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
        assert (bundleSpecificationSatisfied2.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
        assert (!bundleSpecificationUnsatisfied1.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
        assert (!bundleSpecificationUnsatisfied2.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testCountNonsenseConjunction() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<INode> activity = new IsKind(StatementOrBundle.Kind.PROV_ACTIVITY);

        ICondition<INode> notActivity = new IsNotKind(StatementOrBundle.Kind.PROV_ACTIVITY);

        AllTrue<INode> conjunction = new AllTrue<INode>(List.of(
                activity,
                notActivity
        ));

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(conjunction),
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    void testHasTimestampTrue() {
        CpmDocument cpmDoc = TestDocumentProvider.processingBundle_V0;

        ICondition<INode> hasTimestamp = new HasAttrTimestampValue(
                ATTR_START_TIME.getUri(),
                "2021-01-01T00:00:00.000+01:00",
                "2021-02-01T00:00:00.000+01:00",
                "2020-01-01T00:00:00.000+01:00"

        );

        INode node = cpmDoc.getNode(new QualifiedName(BLANK_URI, "MaterialProcessing", "blank"));

        assert (hasTimestamp.test(node));
    }

    @Test
    void testHasTimestampFalse() {
        CpmDocument cpmDoc = TestDocumentProvider.processingBundle_V0;

        ICondition<INode> hasTimestampEqual = new HasAttrTimestampValue(
                ATTR_START_TIME.getUri(),
                "2021-11-01T00:00:00.000+01:00",
                null,
                null

        );

        ICondition<INode> hasTimestampBefore = new HasAttrTimestampValue(
                ATTR_START_TIME.getUri(),
                null,
                "2020-01-01T00:00:00.000+01:00",
                null

        );

        ICondition<INode> hasTimestampAfter = new HasAttrTimestampValue(
                ATTR_START_TIME.getUri(),
                null,
                null,
                "2022-01-01T00:00:00.000+01:00"

        );

        INode node = cpmDoc.getNode(new QualifiedName(BLANK_URI, "MaterialProcessing", "blank"));

        assert (!hasTimestampEqual.test(node));
        assert (!hasTimestampAfter.test(node));
        assert (!hasTimestampBefore.test(node));
    }

    @Test
    void testIsRelation() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<IEdge> isDerivation = new IsRelation(StatementOrBundle.Kind.PROV_DERIVATION);

        assert (cpmDoc.getEdges().stream().anyMatch(edge -> isDerivation.test(edge)));
    }

    @Test
    void testIsNotRelation() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        ICondition<IEdge> isNotDerivation = new IsNotRelation(StatementOrBundle.Kind.PROV_DERIVATION);

        assert (cpmDoc.getEdges().stream().anyMatch(edge -> isNotDerivation.test(edge)));
    }

    @Test
    public void testHasForwardJumpConnectors() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        FindFittingLinearSubgraphs forwardJumpChain = new FindFittingLinearSubgraphs(
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

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                forwardJumpChain,
                EComparisonResult.GREATER_THAN_OR_EQUALS,
                1
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void hasBackwardJumpConnectorTo() {
        CpmDocument cpmDoc = TestDocumentProvider.speciesIdentificationBundle_V0;

        FindFittingLinearSubgraphs backwardJumpChain = new FindFittingLinearSubgraphs(
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

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                backwardJumpChain,
                EComparisonResult.GREATER_THAN_OR_EQUALS,
                1
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testDoesNotHaveForwardJumpConnectors() {
        CpmDocument cpmDoc = TestDocumentProvider.processingBundle_V1;

        FindFittingLinearSubgraphs forwardJumpChain = new FindFittingLinearSubgraphs(
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

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                forwardJumpChain,
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    @Test
    public void testFindPersonNodes() {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        IFindableInDocument<INode> findPersons = new FindFittingNodes(isPerson);

        INode startNode = CpmUtils.chooseStartNode(cpmDoc);

        assert (findPersons.find(cpmDoc, startNode).size() == 2);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testSimpleValiditySpecificationParams")
    public void testSimpleValiditySpecification(CpmDocument cpmDoc, INode startNode) {
        assert (simpleValidityCondition.test(new BundleStart(cpmDoc, startNode)));
    }

    static Stream<Object[]> testSimpleValiditySpecificationParams() {
        return Stream.of(
                new Object[]{TestDocumentProvider.samplingBundle_V0, CpmUtils.chooseStartNode(TestDocumentProvider.samplingBundle_V0)},
                new Object[]{TestDocumentProvider.samplingBundle_V1, CpmUtils.chooseStartNode(TestDocumentProvider.samplingBundle_V1)},
                new Object[]{TestDocumentProvider.processingBundle_V0, CpmUtils.chooseStartNode(TestDocumentProvider.processingBundle_V0)},
                new Object[]{TestDocumentProvider.processingBundle_V1, CpmUtils.chooseStartNode(TestDocumentProvider.processingBundle_V1)},
                new Object[]{TestDocumentProvider.speciesIdentificationBundle_V0, CpmUtils.chooseStartNode(TestDocumentProvider.speciesIdentificationBundle_V0)},
                new Object[]{TestDocumentProvider.dnaSequencingBundle_V0, CpmUtils.chooseStartNode(TestDocumentProvider.dnaSequencingBundle_V0)}
        );
    }

    @Test
    public void testSerializationRoundTrip() throws IOException {
        CpmDocument cpmDoc = TestDocumentProvider.samplingBundle_V1;

        assert (simpleValidityCondition.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));

        Path filePath = Path.of(specificationsFolder, "simpleValidityCondition.json");

        serializeToJson(filePath, simpleValidityCondition);

        ObjectMapper mapper = new ObjectMapper();
        ICondition<BundleStart> deserialized =
                mapper.readValue(
                        filePath.toFile(),
                        new TypeReference<ICondition<BundleStart>>() {
                        }
                );
        assert (deserialized.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
    }

    public void serializeToJson(Path filePath, Object object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        Files.writeString(filePath, json);
    }
}
