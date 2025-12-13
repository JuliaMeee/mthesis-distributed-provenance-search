package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.MockedStorage;
import cz.muni.xmichalk.TestDocumentProvider;
import cz.muni.xmichalk.bundleVersionPicker.EVersionPreference;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.models.QueryContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.model.QualifiedName;

import java.util.stream.Stream;

public class GetPreferredVersionTest {
    static Stream<Object[]> testParams() {
        return Stream.of(
                new Object[]{
                        TestDocumentProvider.samplingBundle_V0,
                        TestDocumentProvider.samplingBundle_V0.getForwardConnectors().getFirst(),
                        EVersionPreference.SPECIFIED,
                        TestDocumentProvider.samplingBundle_V0.getBundleId()},
                new Object[]{
                        TestDocumentProvider.samplingBundle_V0,
                        TestDocumentProvider.samplingBundle_V0.getForwardConnectors().getFirst(),
                        EVersionPreference.LATEST,
                        TestDocumentProvider.samplingBundle_V1.getBundleId()},
                new Object[]{
                        TestDocumentProvider.processingBundle_V0,
                        TestDocumentProvider.processingBundle_V0.getForwardConnectors().getFirst(),
                        EVersionPreference.SPECIFIED,
                        TestDocumentProvider.processingBundle_V0.getBundleId()},
                new Object[]{
                        TestDocumentProvider.processingBundle_V0,
                        TestDocumentProvider.processingBundle_V0.getForwardConnectors().getFirst(),
                        EVersionPreference.LATEST,
                        TestDocumentProvider.processingBundle_V1.getBundleId()}
        );
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testGetPreferredVersion(CpmDocument document, INode startNode, EVersionPreference preference,
                                        QualifiedName expectedBundleId) {
        GetPreferredVersion query = new GetPreferredVersion(preference);
        QueryContext context = new QueryContext(document, startNode, MockedStorage.authTokenFullAccess,
                new MockedStorage());


        QualifiedNameData resultBundleId = query.evaluate(context);

        assert resultBundleId.toQN().getUri().equals(expectedBundleId.getUri());

    }
}
