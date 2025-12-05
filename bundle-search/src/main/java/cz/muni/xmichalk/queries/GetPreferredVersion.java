package cz.muni.xmichalk.queries;

import cz.muni.xmichalk.bundleVersionPicker.EVersionPreference;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.implementations.LatestVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.implementations.SpecifiedVersionPicker;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.models.BundleStart;
import cz.muni.xmichalk.models.QualifiedNameData;
import org.openprovenance.prov.model.QualifiedName;

public class GetPreferredVersion implements IQuery<QualifiedNameData>, IRequiresDocumentLoader {
    private IDocumentLoader documentLoader;
    public EVersionPreference versionPreference;

    public GetPreferredVersion() {
    }

    public GetPreferredVersion(EVersionPreference versionPreference, IDocumentLoader documentLoader) {
        this.versionPreference = versionPreference;
        this.documentLoader = documentLoader;
    }

    public GetPreferredVersion(EVersionPreference versionPreference) {
        this.versionPreference = versionPreference;
    }


    @Override
    public QualifiedNameData evaluate(BundleStart input) {
        if (versionPreference == null) {
            throw new IllegalStateException("Value of versionPreference cannot be null in " + this.getClass().getName());
        }

        IVersionPicker versionPicker = null;

        switch (this.versionPreference) {
            case LATEST:
                versionPicker = new LatestVersionPicker(documentLoader);
                break;
            case SPECIFIED:
                versionPicker = new SpecifiedVersionPicker();
                break;
            default:
                throw new IllegalArgumentException("Unknown version preference: " + this.versionPreference);
        }

        QualifiedName pickedVersion = versionPicker.apply(input.bundle);

        if (pickedVersion != null) {
            return new QualifiedNameData().from(pickedVersion);
        }

        return null;
    }

    @Override
    public void injectDocumentLoader(final IDocumentLoader documentLoader) {
        this.documentLoader = documentLoader;
    }
}
