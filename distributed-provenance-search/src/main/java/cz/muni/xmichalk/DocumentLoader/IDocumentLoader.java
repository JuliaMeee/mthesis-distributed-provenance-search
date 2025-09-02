package cz.muni.xmichalk.DocumentLoader;

import cz.muni.fi.cpm.model.CpmDocument;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public interface IDocumentLoader {
    CpmDocument load(String uri);
}
