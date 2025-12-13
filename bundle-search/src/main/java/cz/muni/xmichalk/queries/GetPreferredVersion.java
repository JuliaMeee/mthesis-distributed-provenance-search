package cz.muni.xmichalk.queries;

import cz.muni.xmichalk.bundleVersionPicker.EVersionPreference;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.implementations.LatestVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.implementations.SpecifiedVersionPicker;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.storage.EBundlePart;
import org.openprovenance.prov.model.QualifiedName;

public class GetPreferredVersion implements IQuery<QualifiedNameData> {
    public EVersionPreference versionPreference;

    public GetPreferredVersion() {
    }

    public GetPreferredVersion(EVersionPreference versionPreference) {
        this.versionPreference = versionPreference;
    }


    @Override
    public QualifiedNameData evaluate(QueryContext context) {
        if (versionPreference == null) {
            throw new IllegalStateException(
                    "Value of versionPreference cannot be null in " + this.getClass().getName());
        }

        IVersionPicker versionPicker = null;

        switch (this.versionPreference) {
            case LATEST:
                versionPicker = new LatestVersionPicker(context.documentLoader, context.authorizationHeader);
                break;
            case SPECIFIED:
                versionPicker = new SpecifiedVersionPicker();
                break;
            default:
                throw new IllegalArgumentException("Unknown version preference: " + this.versionPreference);
        }

        QualifiedName pickedVersion = versionPicker.apply(context.document);

        if (pickedVersion != null) {
            return new QualifiedNameData().from(pickedVersion);
        }

        return null;
    }

    @Override
    public EBundlePart decideRequiredBundlePart() {
        return EBundlePart.TraversalInformation;
    }
}
