package cz.muni.xmichalk.documentLoader;

import cz.muni.xmichalk.documentLoader.storageDTO.Token;
import org.openprovenance.prov.model.Document;

public class StorageDocument {
    public Document document;
    public Token token;

    public StorageDocument(Document document, Token token) {
        this.document = document;
        this.token = token;
    }
}
