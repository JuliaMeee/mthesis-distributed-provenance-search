package cz.muni.xmichalk.DTO;

import cz.muni.fi.cpm.model.INode;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class ResponseDTO {
    public String connectors;
    public Object found;
    
    public ResponseDTO(List<INode> connectors, Object found, Document provDoc) {
        this.connectors = serializeConnectorsIntoDocument(connectors, provDoc);
        this.found = found;
    }
    
    public String serializeConnectorsIntoDocument(List<INode> connectors, Document provDoc){
        ProvFactory pf = ProvFactory.getFactory();
        Document doc = pf.newDocument();

        Namespace ns = provDoc.getNamespace();
        ns.addKnownNamespaces();
        ns.register("ex", "http://example.org/");
        doc.setNamespace(ns);
        
        var bundle = pf.newNamedBundle(pf.newQualifiedName("http://example.org/", "connectors_bundle", "ex"), null);
        
        for (INode node : connectors) {
            var elements = node.getElements();
            
            for (var element : elements) {
                bundle.getStatement().add(element);
            }
        }

        bundle.setNamespace(ns);
        doc.getStatementOrBundle().add(bundle);

        var interop = new InteropFramework();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        interop.writeDocument(outputStream, doc, InteropFramework.MEDIA_APPLICATION_JSON, false);
        return outputStream.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
