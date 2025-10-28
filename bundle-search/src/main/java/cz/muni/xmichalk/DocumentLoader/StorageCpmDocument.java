package cz.muni.xmichalk.DocumentLoader;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.Token;
import org.openprovenance.prov.model.Document;

public class StorageCpmDocument {
    public CpmDocument document;
    public Token token;

    public StorageCpmDocument(CpmDocument document, Token token) {
        this.document = document;
        this.token = token;
    }
}
