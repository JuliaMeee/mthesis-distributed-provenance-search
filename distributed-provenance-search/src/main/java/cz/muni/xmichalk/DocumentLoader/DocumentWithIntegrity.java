package cz.muni.xmichalk.DocumentLoader;

import cz.muni.fi.cpm.model.CpmDocument;
import org.openprovenance.prov.model.Document;

public class DocumentWithIntegrity {
    public Document document;
    public boolean integrityCheckPassed;

    public DocumentWithIntegrity(Document document, boolean integrityCheckPassed) {
        this.document = document;
        this.integrityCheckPassed = integrityCheckPassed;
    }
}
