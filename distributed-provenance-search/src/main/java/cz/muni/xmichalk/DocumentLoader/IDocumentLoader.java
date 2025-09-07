package cz.muni.xmichalk.DocumentLoader;

public interface IDocumentLoader {
    DocumentWithIntegrity loadDocument(String uri);

    DocumentWithIntegrity loadMetaDocument(String uri);
}
