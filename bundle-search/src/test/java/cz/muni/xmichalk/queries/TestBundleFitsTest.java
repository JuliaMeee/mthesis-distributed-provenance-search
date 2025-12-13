package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.TestDocumentProvider;
import cz.muni.xmichalk.models.QueryContext;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.stream.Stream;

public class TestBundleFitsTest {
    static Stream<Object[]> testParams() {
        return Stream.of(
                new Object[]{TestDocumentProvider.samplingBundle_V1,
                        TestDocumentProvider.samplingBundle_V1.getForwardConnectors().getFirst()},
                new Object[]{TestDocumentProvider.processingBundle_V1,
                        TestDocumentProvider.processingBundle_V1.getForwardConnectors().getFirst()}
        );
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testBundleFits_true(CpmDocument cpmDocument, INode startNode) {
        TestBundleFits testBundleFits = new TestBundleFits(
                (bundleStart) -> bundleStart.startNode == startNode
        );
        QueryContext context = new QueryContext(cpmDocument, startNode, null, null);

        assert testBundleFits.evaluate(context);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testBundleFits_false(CpmDocument cpmDocument, INode startNode) {
        TestBundleFits testBundleFits = new TestBundleFits(
                (bundleStart) -> false
        );
        QueryContext context = new QueryContext(cpmDocument, startNode, null, null);

        assert !testBundleFits.evaluate(context);
    }
}
