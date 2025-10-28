package cz.muni.xmichalk.DocumentLoader;

public interface IDocumentLoader {
    StorageDocument loadDocument(String uri);
    StorageCpmDocument loadCpmDocument(String uri);

    StorageDocument loadMetaDocument(String uri);
    StorageCpmDocument loadMetaCpmDocument(String uri);
}
