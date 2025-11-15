package cz.muni.xmichalk.bundleVersionPicker.pickerImplementations;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.util.BundleNodesTraverser;
import cz.muni.xmichalk.util.CpmUtils;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_VERSION;
import static cz.muni.xmichalk.util.NameSpaceConstants.PROV_URI;

public class LatestVersionPicker implements IVersionPicker {
    private final IDocumentLoader documentLoader;

    public LatestVersionPicker(IDocumentLoader documentLoader) {
        this.documentLoader = documentLoader;
    }

    @Override
    public QualifiedName apply(QualifiedName bundleId) {
        var bundle = documentLoader.loadCpmDocument(bundleId.getUri());

        if (bundle == null || bundle.document == null) {
            throw new RuntimeException("Failed to load bundle document for id: " + bundleId);
        }

        var metaBundleId = CpmUtils.getMetaBundleId(bundle.document);

        if (metaBundleId == null) {
            throw new RuntimeException("Failed to find meta bundle reference in bundle: " + bundleId);
        }

        var metaDocument = documentLoader.loadMetaCpmDocument(metaBundleId.getUri());

        if (metaDocument == null || metaDocument.document == null) {
            throw new RuntimeException("Failed to load meta bundle: " + metaBundleId);
        }

        return pickFrom(bundleId, metaDocument.document);
    }

    public static QualifiedName pickFrom(QualifiedName bundleId, CpmDocument metaDocument) {
        var versionNode = pickVersionNode(metaDocument);

        if (versionNode == null) {
            return bundleId;
        }

        return versionNode.getId();
    }

    private static INode pickVersionNode(CpmDocument metaDocument) {
        var versionNodes = BundleNodesTraverser.traverseAndFind(
                metaDocument,
                metaDocument.getNodes().getFirst().getId(),
                node -> hasProvTypeBundle(node) && hasVersionAttribute(node)
        );

        if (versionNodes == null || versionNodes.isEmpty()) {
            return null;
        }

        int latestVersion = 0;
        INode latestVersionNode = null;

        for (INode node : versionNodes) {
            var versionString = ((LangString) CpmUtils.getAttributeValue(node, ATTR_VERSION)).getValue();
            var version = Double.parseDouble(versionString);

            if (version > latestVersion) {
                latestVersion = (int) version;
                latestVersionNode = node;
            }
        }

        return latestVersionNode;
    }


    private static boolean hasProvTypeBundle(INode node) {
        return CpmUtils.hasAttributeTargetValue(node, ATTR_PROV_TYPE, QualifiedName.class, qn ->
                qn.getUri().equals(PROV_URI + "bundle")
        );
    }

    private static boolean hasVersionAttribute(INode node) {
        return CpmUtils.hasAttributeTargetValue(node, ATTR_VERSION, LangString.class, v -> true);
    }
}
