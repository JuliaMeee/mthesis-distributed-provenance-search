package cz.muni.xmichalk.traverser;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.xmichalk.TestBundleData;
import cz.muni.xmichalk.dto.BundleQueryResultDTO;
import cz.muni.xmichalk.dto.ConnectorDTO;
import cz.muni.xmichalk.dto.QualifiedNameDTO;
import cz.muni.xmichalk.dto.token.Token;
import cz.muni.xmichalk.integrity.IIntegrityVerifier;
import cz.muni.xmichalk.models.ItemToTraverse;
import cz.muni.xmichalk.models.TraversalParams;
import cz.muni.xmichalk.models.TraversalResults;
import cz.muni.xmichalk.provServiceAPI.IProvServiceAPI;
import cz.muni.xmichalk.provServiceTable.IProvServiceTable;
import cz.muni.xmichalk.traversalPriority.ETraversalPriority;
import cz.muni.xmichalk.traversalPriority.IntegrityThenOrderedValidity;
import cz.muni.xmichalk.validity.EValidityCheck;
import cz.muni.xmichalk.validity.IValidityVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.model.QualifiedName;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class TraverserTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXAMPLE_NAMESPACE_URI = "http://example.org/";
    private static final String EXAMPLE_PREFIX = "example";

    private static final QualifiedName bundleA = getExampleQN("bundleA");
    private static final QualifiedName bundleA_new = getExampleQN("bundleA_new");
    private static final QualifiedName bundleB = getExampleQN("bundleB");
    private static final QualifiedName bundleC = getExampleQN("bundleC");
    private static final QualifiedName bundleD = getExampleQN("bundleD");


    private static final QualifiedName connA1 = getExampleQN("connA1");
    private static final QualifiedName connA2 = getExampleQN("connA2");
    private static final QualifiedName connA3 = getExampleQN("connA3");
    private static final QualifiedName connB1 = getExampleQN("connB1");
    private static final QualifiedName connB2 = getExampleQN("connB2");
    private static final QualifiedName connC1 = getExampleQN("connC1");
    private static final QualifiedName connD1 = getExampleQN("connD1");

    private static final Map<String, TestBundleData> testDataSet1 = Map.of(
            bundleA.getUri(), new TestBundleData(
                    bundleA,
                    bundleA_new,
                    objectMapper.valueToTree("bundleA_result"),
                    List.of(),
                    List.of()
            ),
            bundleA_new.getUri(), new TestBundleData(
                    bundleA_new,
                    bundleA_new,
                    objectMapper.valueToTree("bundleA_new_result"),
                    List.of(),
                    List.of(
                            getConnectorData(connA1, bundleB),
                            getConnectorData(connA2, bundleC)
                    )
            ),
            bundleB.getUri(), new TestBundleData(
                    bundleB,
                    bundleB,
                    objectMapper.valueToTree("bundleB_result"),
                    List.of(
                            getConnectorData(connA1, bundleA)
                    ),
                    List.of(
                            getConnectorData(connB1, bundleD)
                    )
            ),
            bundleC.getUri(), new TestBundleData(
                    bundleC,
                    bundleC,
                    objectMapper.valueToTree("bundleC_result"),
                    List.of(
                            getConnectorData(connA2, bundleA)
                    ),
                    List.of(
                            getConnectorData(connC1, bundleD)
                    )
            ),
            bundleD.getUri(), new TestBundleData(
                    bundleD,
                    bundleD,
                    objectMapper.valueToTree("bundleD_result"),
                    List.of(
                            getConnectorData(connB1, bundleB),
                            getConnectorData(connC1, bundleC)
                    ),
                    List.of()
            )
    );

    private static QualifiedName getExampleQN(String localName) {
        return new org.openprovenance.prov.vanilla.QualifiedName(EXAMPLE_NAMESPACE_URI, localName, EXAMPLE_PREFIX);
    }

    private static ConnectorDTO getConnectorData(QualifiedName connId, QualifiedName referencedBundleId) {
        return new ConnectorDTO(
                new QualifiedNameDTO().from(connId),
                new QualifiedNameDTO().from(connId),
                new QualifiedNameDTO().from(referencedBundleId),
                new QualifiedNameDTO().from(getExampleQN("meta_" + referencedBundleId.getLocalPart())),
                "hashA",
                "SHA-256",
                "http://provservice.com/" + referencedBundleId.getLocalPart()
        );
    }

    private static IProvServiceTable getMockedProvServiceTable() {
        return (bundleUri) -> "http://provService.com/".concat(bundleUri);
    }

    private static IProvServiceAPI getMockedProvServiceAPI(Map<String, TestBundleData> testData) {
        return new IProvServiceAPI() {
            @Override
            public BundleQueryResultDTO fetchBundleQueryResult(final String serviceUri, final QualifiedName bundleId,
                                                               final QualifiedName connectorId,
                                                               final JsonNode querySpecification) {
                return new BundleQueryResultDTO(new Token(null, "x"), testData.get(bundleId.getUri()).queryResult);
            }

            @Override
            public QualifiedName fetchPreferredBundleVersion(final String serviceUri, final QualifiedName bundleId,
                                                             final QualifiedName connectorId,
                                                             final String versionPreference) {
                if (versionPreference.equals("LATEST")) {
                    return testData.get(bundleId.getUri()).latestVersionId;
                }
                return bundleId;
            }

            @Override
            public BundleQueryResultDTO fetchBundleConnectors(final String serviceUri, final QualifiedName bundleId,
                                                              final QualifiedName connectorId, final boolean backward) {
                List<ConnectorDTO> connectors = backward ? testData.get(
                        bundleId.getUri()).backwardConnectors : testData.get(bundleId.getUri()).forwardConnectors;

                return new BundleQueryResultDTO(new Token(null, "x"), objectMapper.valueToTree(connectors));
            }
        };
    }

    private static IIntegrityVerifier getMockedIntegrityVerifier(List<QualifiedName> failForBundles) {
        return (bundleId, _) -> failForBundles.stream().noneMatch(b -> b.getUri().equals(bundleId.getUri()));
    }

    private static Map<EValidityCheck, IValidityVerifier> getMockedValidityVerifiers(
            Map<QualifiedName, EValidityCheck> failForBundles) {
        BiPredicate<QualifiedName, EValidityCheck> validate =
                (QualifiedName bundleId, EValidityCheck validityCheck) -> failForBundles.entrySet()
                        .stream().noneMatch(
                                e -> e.getKey().getUri().equals(bundleId.getUri()) &&
                                        e.getValue() == validityCheck
                        );

        return Map.of(
                EValidityCheck.DEMO_SIMPLE_CONSTRAINTS, (itemToTraverse, _) ->
                        validate.test(itemToTraverse.bundleId, EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                EValidityCheck.DEMO_IS_SAMPLING_BUNDLE, (itemToTraverse, _) ->
                        validate.test(itemToTraverse.bundleId, EValidityCheck.DEMO_IS_SAMPLING_BUNDLE),
                EValidityCheck.DEMO_IS_PROCESSING_BUNDLE, (itemToTraverse, _) ->
                        validate.test(itemToTraverse.bundleId, EValidityCheck.DEMO_IS_PROCESSING_BUNDLE)
        );
    }

    private static Map<ETraversalPriority, Comparator<ItemToTraverse>> getMockedPriorityComparators() {
        return Map.of(
                ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                new IntegrityThenOrderedValidity()
        );
    }

    static Stream<Object[]> bothDirectionsPrams() {
        return Stream.of(
                new Object[]{true, bundleD, connD1, "LATEST", 4},
                new Object[]{false, bundleA, connA1, "LATEST", 4},

                new Object[]{true, bundleD, connD1, "SPECIFIED", 4},
                new Object[]{false, bundleA, connA1, "SPECIFIED", 1},

                new Object[]{false, bundleB, connB1, "LATEST", 2},
                new Object[]{true, bundleC, connC1, "SPECIFIED", 2}
        );
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("bothDirectionsPrams")
    public void test_traverseCount(boolean backward, QualifiedName startBundle, QualifiedName startConnector,
                                   String versionPreference, int expectedCount) {
        Traverser traverser = new Traverser(
                getMockedProvServiceTable(),
                getMockedProvServiceAPI(testDataSet1),
                getMockedIntegrityVerifier(List.of()),
                10,
                true,
                getMockedValidityVerifiers(Map.of()),
                getMockedPriorityComparators()
        );

        TraversalResults results = traverser.traverseChain(
                startBundle,
                startConnector,
                new TraversalParams(
                        backward,
                        versionPreference,
                        ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                        List.of(EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                        null
                )
        );

        assert results.results.size() == expectedCount;
        assert results.results.stream().allMatch(r -> r.integrity);
        assert results.results.stream().allMatch(r -> r.validityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.results.stream().allMatch(r -> r.pathIntegrity);
        assert results.results.stream().allMatch(r -> r.pathValidityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.errors.isEmpty();
    }

    @Test
    public void test_failedIntegrity() {

        Traverser traverser = new Traverser(
                getMockedProvServiceTable(),
                getMockedProvServiceAPI(testDataSet1),
                getMockedIntegrityVerifier(List.of(bundleB)),
                10,
                true,
                getMockedValidityVerifiers(Map.of()),
                getMockedPriorityComparators()
        );

        TraversalResults results = traverser.traverseChain(
                bundleA,
                connA1,
                new TraversalParams(
                        false,
                        "LATEST",
                        ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                        List.of(EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                        null
                )
        );

        assert results.results.size() == 4;
        assert results.results.stream().filter(r -> r.integrity).count() == 3;
        assert results.results.stream().allMatch(r -> r.validityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.results.stream().allMatch(r -> r.pathIntegrity);
        assert results.results.stream().allMatch(r -> r.pathValidityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.errors.isEmpty();

        // Access captured logs
        List<ILoggingEvent> logs = logAppender.getLogs();
        logs.forEach(log -> System.out.println(log.getFormattedMessage()));

    }

    @Test
    public void test_failedPathIntegrity() {

        Traverser traverser = new Traverser(
                getMockedProvServiceTable(),
                getMockedProvServiceAPI(testDataSet1),
                getMockedIntegrityVerifier(List.of(bundleB, bundleC)),
                10,
                true,
                getMockedValidityVerifiers(Map.of()),
                getMockedPriorityComparators()
        );

        TraversalResults results = traverser.traverseChain(
                bundleA,
                connA1,
                new TraversalParams(
                        false,
                        "LATEST",
                        ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                        List.of(EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                        null
                )
        );

        assert results.results.size() == 4;
        assert results.results.stream().filter(r -> r.integrity).count() == 2;
        assert results.results.stream().allMatch(r -> r.validityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.results.stream().filter(r -> r.pathIntegrity).count() == 3;
        assert results.results.stream().allMatch(r -> r.pathValidityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.errors.isEmpty();
    }

    @Test
    public void test_failedValidity() {
        Traverser traverser = new Traverser(
                getMockedProvServiceTable(),
                getMockedProvServiceAPI(testDataSet1),
                getMockedIntegrityVerifier(List.of()),
                10,
                true,
                getMockedValidityVerifiers(Map.of(
                        bundleB, EValidityCheck.DEMO_SIMPLE_CONSTRAINTS
                )),
                getMockedPriorityComparators()
        );

        TraversalResults results = traverser.traverseChain(
                bundleD,
                connD1,
                new TraversalParams(
                        true,
                        "LATEST",
                        ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                        List.of(EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                        null
                )
        );

        assert results.results.size() == 4;
        assert results.results.stream().allMatch(r -> r.integrity);
        assert results.results.stream().filter(r -> r.validityChecks.stream().allMatch(Map.Entry::getValue))
                .count() == 3;
        assert results.results.stream().allMatch(r -> r.pathIntegrity);
        assert results.results.stream().allMatch(r -> r.pathValidityChecks.stream().allMatch(Map.Entry::getValue));
        assert results.errors.isEmpty();
    }

    @Test
    public void test_failedPathValidity() {
        Traverser traverser = new Traverser(
                getMockedProvServiceTable(),
                getMockedProvServiceAPI(testDataSet1),
                getMockedIntegrityVerifier(List.of()),
                10,
                true,
                getMockedValidityVerifiers(Map.of(
                        bundleB, EValidityCheck.DEMO_SIMPLE_CONSTRAINTS,
                        bundleC, EValidityCheck.DEMO_SIMPLE_CONSTRAINTS
                )),
                getMockedPriorityComparators()
        );

        TraversalResults results = traverser.traverseChain(
                bundleD,
                connD1,
                new TraversalParams(
                        true,
                        "LATEST",
                        ETraversalPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS,
                        List.of(EValidityCheck.DEMO_SIMPLE_CONSTRAINTS),
                        null
                )
        );

        assert results.results.size() == 4;
        assert results.results.stream().allMatch(r -> r.integrity);
        assert results.results.stream().filter(r -> r.validityChecks.stream().allMatch(Map.Entry::getValue))
                .count() == 2;
        assert results.results.stream().allMatch(r -> r.pathIntegrity);
        assert results.results.stream().filter(r -> r.pathValidityChecks.stream().allMatch(Map.Entry::getValue))
                .count() == 3;
        assert results.errors.isEmpty();
    }

    private TestLogAppender logAppender;
    private Logger traverserLogger;

    static class TestLogAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> logs = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            logs.add(eventObject);
        }

        public List<ILoggingEvent> getLogs() {
            return logs;
        }
    }

    @BeforeEach
    void setupLogger() {
        traverserLogger = (Logger) LoggerFactory.getLogger("cz.muni.xmichalk.traverser.Traverser");
        logAppender = new TestLogAppender();
        logAppender.start();
        traverserLogger.addAppender(logAppender);
    }
}
