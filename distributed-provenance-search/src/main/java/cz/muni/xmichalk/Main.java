package cz.muni.xmichalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageDTO.StorageResponse;
import cz.muni.xmichalk.DocumentLoader.StorageDocumentLoader;
import cz.muni.xmichalk.Traverser.Traverser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.openprovenance.prov.vanilla.QualifiedName;

import javax.xml.transform.Result;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Main {
    public static void main(String[] args) {

        System.out.println("Hello, World!");

        ProvFactory pf = new ProvFactory();
        CpmMergedFactory cf = new CpmMergedFactory();
        CpmProvFactory cpf = new CpmProvFactory(pf);

        IDocumentLoader documentLoader = new StorageDocumentLoader(pf, cf, cpf, Formats.ProvFormat.JSON);
        Traverser traverser = new Traverser(documentLoader);
        try {
            traverser.searchBackward(
                            new QualifiedName("http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/", "DnaSequencingBundle_V0", "storage2"),
                            new QualifiedName("https://openprovenance.org/blank/", "FilteredSequencesCon", "blank"),
                            node -> node.getId().getLocalPart().equals("ABSPermit_ircc2345678"))
                    .forEach(result -> System.out.println(result.bundleId.getLocalPart() + " : " + result.node.getId().getLocalPart()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}