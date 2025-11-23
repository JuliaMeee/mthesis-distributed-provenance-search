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

    public GetPreferredVersion(IDocumentLoader documentLoader, EVersionPreference versionPreference) {
        this.documentLoader = documentLoader;
        this.versionPreference = versionPreference;
    }

    public GetPreferredVersion(EVersionPreference versionPreference) {
        this.versionPreference = versionPreference;
    }


    @Override
    public QualifiedNameData evaluate(BundleStart input) {
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
