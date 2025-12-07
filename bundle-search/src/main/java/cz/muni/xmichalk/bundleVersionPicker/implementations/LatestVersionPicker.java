package cz.muni.xmichalk.bundleVersionPicker.implementations;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageCpmDocument;
import cz.muni.xmichalk.util.AttributeUtils;
import cz.muni.xmichalk.util.CpmUtils;
import cz.muni.xmichalk.util.GraphTraverser;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.QualifiedName;

import java.util.Set;

import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_VERSION;
import static cz.muni.xmichalk.util.NameSpaceConstants.PROV_URI;

public class LatestVersionPicker implements IVersionPicker {
    private final IDocumentLoader documentLoader;

    public LatestVersionPicker(IDocumentLoader documentLoader) {
        this.documentLoader = documentLoader;
    }

    @Override
    public QualifiedName apply(CpmDocument bundle) {
        if (bundle == null) {
            throw new RuntimeException("Bundle document is null");
        }
        if (documentLoader == null) {
            throw new RuntimeException("Document loader is null");
        }

        QualifiedName metaBundleId = CpmUtils.getMetaBundleId(bundle);

        if (metaBundleId == null) {
            throw new RuntimeException(
                    "Failed to find meta bundle reference in bundle: " + bundle.getBundleId().getUri());
        }

        StorageCpmDocument metaDocument = documentLoader.loadMetaCpmDocument(metaBundleId.getUri());

        if (metaDocument == null || metaDocument.document == null) {
            throw new RuntimeException("Failed to load meta bundle: " + metaBundleId.getUri());
        }

        INode versionNode = pickLatestVersionNode(metaDocument.document);

        return versionNode != null ? versionNode.getId() : null;
    }

    public static INode pickLatestVersionNode(CpmDocument metaDocument) {

        Set<INode> versionNodes = GraphTraverser.traverseAndFindNodes(
                metaDocument.getNodes().getFirst(),
                (node) -> hasProvTypeBundle(node) && hasVersionAttribute(node)
        );

        if (versionNodes == null || versionNodes.isEmpty()) {
            return null;
        }

        int latestVersion = 0;
        INode latestVersionNode = null;

        for (INode node : versionNodes) {
            String versionString = ((LangString) AttributeUtils.getAttributeValue(node, ATTR_VERSION)).getValue();
            double version = Double.parseDouble(versionString);

            if (version > latestVersion) {
                latestVersion = (int) version;
                latestVersionNode = node;
            }
        }

        return latestVersionNode;
    }


    private static boolean hasProvTypeBundle(INode node) {
        return AttributeUtils.hasAttributeTargetValue(node, ATTR_PROV_TYPE, QualifiedName.class, qn ->
                qn.getUri().equals(PROV_URI + "bundle")
        );
    }

    private static boolean hasVersionAttribute(INode node) {
        return AttributeUtils.hasAttributeTargetValue(node, ATTR_VERSION, LangString.class, v -> true);
    }
}
