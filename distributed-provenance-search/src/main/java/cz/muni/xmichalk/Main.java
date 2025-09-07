package cz.muni.xmichalk;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageDocumentLoader;
import cz.muni.xmichalk.Traverser.Traverser;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.openprovenance.prov.vanilla.QualifiedName;

public class Main {
    public static void main(String[] args) {

        System.out.println("Hello, World!");

        ProvFactory pf = new ProvFactory();
        CpmMergedFactory cf = new CpmMergedFactory();
        CpmProvFactory cpf = new CpmProvFactory(pf);

        IDocumentLoader documentLoader = new StorageDocumentLoader();
        Traverser traverser = new Traverser(documentLoader, pf, cf, cpf);
        try {

            traverser.loadMetaBundle("http://prov-storage-1:8000/api/v1/documents/meta/SamplingBundle_V0_meta");

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