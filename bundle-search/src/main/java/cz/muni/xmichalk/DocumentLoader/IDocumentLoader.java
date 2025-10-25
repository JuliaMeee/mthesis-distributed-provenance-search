package cz.muni.xmichalk.DocumentLoader;

public interface IDocumentLoader {
    StorageDocument loadDocument(String uri);

    StorageDocument loadMetaDocument(String uri);
}
