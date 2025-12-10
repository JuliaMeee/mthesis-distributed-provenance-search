package cz.muni.xmichalk.documentLoader;

public interface IDocumentLoader {
    StorageCpmDocument loadCpmDocument(String uri, EBundlePart part);

    StorageCpmDocument loadMetaCpmDocument(String uri);
}
