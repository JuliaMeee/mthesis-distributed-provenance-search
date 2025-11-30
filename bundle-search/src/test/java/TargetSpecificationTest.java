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
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.vanilla.QualifiedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cz.muni.xmichalk.util.AttributeNames.*;
import static cz.muni.xmichalk.util.NameSpaceConstants.*;

public class TargetSpecificationTest extends TestDocumentProvider {

    String specificationsFolder = System.getProperty("user.dir") + "/src/test/resources/targetSpecifications/";

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

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    public void testCountNonsenseNodes() {
        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(nonsenseNodeCondition),
                EComparisonResult.EQUALS,
                0
        );

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    public void testHasSpecificMetaBundleId() {
        ICondition<INode> mainActivityWithMetaRef = new AllTrue<>(List.of(
                isMainActivity,
                isActivityWithMetaRef
        ));

        ICondition<BundleStart> bundleSpecification = new CountCondition(
                new FindFittingNodes(mainActivityWithMetaRef),
                EComparisonResult.EQUALS,
                1
        );

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    public void testCountSatisfiedAllTrue() {
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

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    public void testAllNodesImplicationAnyTrue() {
        // Test that every connector is either a forward connector or has a referenced bundle id and meta bundle id defined

        AnyTrue<INode> backwardOrForwardCon = new AnyTrue<INode>(List.of(
                hasBundleRefAndMetaRef,
                isForwardConn)
        );

        Implication<INode> conImplication = new Implication<INode>(
                isConnector,
                backwardOrForwardCon
        );

        ICondition<BundleStart> bundleSpecification = new AllNodes(conImplication);

        assert (bundleSpecification.test(new BundleStart(processingBundle_V0, CpmUtils.chooseStartNode(processingBundle_V0))));
    }

    @Test
    public void testEither() {
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

        assert (bundleSpecificationSatisfied1.test(new BundleStart(processingBundle_V0, CpmUtils.chooseStartNode(processingBundle_V0))));
        assert (bundleSpecificationSatisfied2.test(new BundleStart(processingBundle_V0, CpmUtils.chooseStartNode(processingBundle_V0))));
        assert (!bundleSpecificationUnsatisfied1.test(new BundleStart(processingBundle_V0, CpmUtils.chooseStartNode(processingBundle_V0))));
        assert (!bundleSpecificationUnsatisfied2.test(new BundleStart(processingBundle_V0, CpmUtils.chooseStartNode(processingBundle_V0))));
    }

    @Test
    public void testCountNonsenseConjunction() {
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

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    void testHasTimestampTrue() {
        ICondition<INode> hasTimestamp = new HasAttrTimestampValue(
                ATTR_START_TIME.getUri(),
                "2021-01-01T00:00:00.000+01:00",
                "2021-02-01T00:00:00.000+01:00",
                "2020-01-01T00:00:00.000+01:00"

        );

        INode node = processingBundle_V0.getNode(new QualifiedName(BLANK_URI, "MaterialProcessing", "blank"));

        assert (hasTimestamp.test(node));
    }

    @Test
    void testHasTimestampFalse() {
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

        INode node = processingBundle_V0.getNode(new QualifiedName(BLANK_URI, "MaterialProcessing", "blank"));

        assert (!hasTimestampEqual.test(node));
        assert (!hasTimestampAfter.test(node));
        assert (!hasTimestampBefore.test(node));
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
    public void testHasForwardJumpConnectors() {
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

        assert (bundleSpecification.test(new BundleStart(samplingBundle_V1, CpmUtils.chooseStartNode(samplingBundle_V1))));
    }

    @Test
    public void hasBackwardJumpConnectorTo() {
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

        assert (bundleSpecification.test(new BundleStart(speciesIdentificationBundle_V0, CpmUtils.chooseStartNode(speciesIdentificationBundle_V0))));
    }

    @Test
    public void testDoesNotHaveForwardJumpConnectors() {
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

        assert (bundleSpecification.test(new BundleStart(processingBundle_V1, CpmUtils.chooseStartNode(processingBundle_V1))));
    }

    @Test
    public void testFindPersonNodes() {
        IFindableInDocument<INode> findPersons = new FindFittingNodes(isPerson);

        INode startNode = CpmUtils.chooseStartNode(samplingBundle_V1);

        assert (findPersons.find(samplingBundle_V1, startNode).size() == 2);
    }


    @Test
    public void testSimpleValiditySpecification() {
        List<CpmDocument> cpmDocs = List.of(
                samplingBundle_V0,
                samplingBundle_V1,
                processingBundle_V0,
                processingBundle_V1,
                speciesIdentificationBundle_V0,
                dnaSequencingBundle_V0
        );

        for (CpmDocument cpmDoc : cpmDocs) {
            assert (simpleValidityCondition.test(new BundleStart(cpmDoc, CpmUtils.chooseStartNode(cpmDoc))));
        }
    }

    @Test
    public void testSerializationRoundTrip() throws IOException {
        CpmDocument cpmDoc = samplingBundle_V1;

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
