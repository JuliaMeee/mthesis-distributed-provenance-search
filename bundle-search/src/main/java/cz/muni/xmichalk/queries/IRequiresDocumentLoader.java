package cz.muni.xmichalk.queries;

import cz.muni.xmichalk.documentLoader.IDocumentLoader;

public interface IRequiresDocumentLoader {
    void injectDocumentLoader(IDocumentLoader documentLoader);
}
