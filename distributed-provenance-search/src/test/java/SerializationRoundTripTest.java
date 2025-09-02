import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.template.mapper.ITemplateProvMapper;
import cz.muni.fi.cpm.template.mapper.TemplateProvMapper;
import cz.muni.fi.cpm.template.schema.*;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class SerializationRoundTripTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/serialization/";


    private Document getTestDocument1() {
        QualifiedName entityId = cPF.newCpmQualifiedName("entity1");
        Entity entity = cPF.getProvFactory().newEntity(entityId);

        QualifiedName agentId = cPF.newCpmQualifiedName("agent1");
        Agent agent = cPF.getProvFactory().newAgent(agentId);

        QualifiedName activityId = cPF.newCpmQualifiedName("activity1");
        Activity activity = cPF.getProvFactory().newActivity(activityId);
        activity.setStartTime(pF.newISOTime("2025-08-16T10:00:00Z"));
        activity.setEndTime(pF.newISOTime("2025-08-16T11:00:00Z"));

        Relation relation = cPF.getProvFactory().newWasAttributedTo(cPF.newCpmQualifiedName("attr"), entityId, agentId);
        Relation relation2 = cPF.getProvFactory().newWasAssociatedWith(cPF.newCpmQualifiedName("assoc"), activityId, agentId);
        Relation relation3 = cPF.getProvFactory().newWasGeneratedBy(cPF.newCpmQualifiedName("gen"), entityId, activityId);

        QualifiedName bundleId = pF.newQualifiedName("www.example.com/", "bundle1", "ex");

        CpmDocument cpmDocument = new CpmDocument(List.of(entity, agent, activity, relation, relation2, relation3), bundleId, pF, cPF, cF);
        return cpmDocument.toDocument();
    }

    private Document getTestDocument2() throws DatatypeConfigurationException {
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        ProvFactory pF = new org.openprovenance.prov.vanilla.ProvFactory();
        TraversalInformation ti = new TraversalInformation();

        ti.setPrefixes(Map.of("ex", "www.example.com/"));
        ti.setBundleName(ti.getNamespace().qualifiedName("ex", "bundle2", pF));

        MainActivity mA = new MainActivity(ti.getNamespace().qualifiedName("ex", "activity1", pF));
        mA.setStartTime(datatypeFactory.newXMLGregorianCalendar("2011-11-16T16:05:00"));
        mA.setEndTime(datatypeFactory.newXMLGregorianCalendar("2011-11-16T18:05:00"));
        ti.setMainActivity(mA);

        QualifiedName bcID = ti.getNamespace().qualifiedName("ex", "backConnector1", pF);
        BackwardConnector bC = new BackwardConnector(bcID);
        ti.getBackwardConnectors().add(bC);

        MainActivityUsed used = new MainActivityUsed(bcID);
        mA.setUsed(List.of(used));

        QualifiedName fcID = ti.getNamespace().qualifiedName("ex", "forwardConnector1", pF);
        mA.setGenerated(List.of(fcID));

        ForwardConnector fC = new ForwardConnector(fcID);
        fC.setDerivedFrom(List.of(bC.getId()));
        ti.getForwardConnectors().add(fC);

        ITemplateProvMapper mapper = new TemplateProvMapper(new CpmProvFactory(pF));
        return mapper.map(ti);
    }

    /*public Document getTestDocument3() {
        QualifiedName entity1Id = cPF.newCpmQualifiedName("entity1");
        Entity entity1 = cPF.getProvFactory().newEntity(entity1Id);

        QualifiedName entity2Id = cPF.newCpmQualifiedName("entity2");
        Entity entity2 = cPF.getProvFactory().newEntity(entity2Id);

        QualifiedName entity3Id = cPF.newCpmQualifiedName("entity3");
        Entity entity3 = cPF.getProvFactory().newEntity(entity3Id);

        QualifiedName agentId = cPF.newCpmQualifiedName("agent1");
        Agent agent = cPF.getProvFactory().newAgent(agentId);

        QualifiedName activity1Id = cPF.newCpmQualifiedName("activity1");
        Activity activity1 = cPF.getProvFactory().newActivity(activity1Id);
        activity1.setStartTime(pF.newISOTime("2025-08-16T10:00:00Z"));
        activity1.setEndTime(pF.newISOTime("2025-08-16T11:00:00Z"));

        QualifiedName activity2Id = cPF.newCpmQualifiedName("activity2");
        Activity activity2 = cPF.getProvFactory().newActivity(activity2Id);
        activity2.setStartTime(pF.newISOTime("2025-08-17T10:00:00Z"));
        activity2.setEndTime(pF.newISOTime("2025-08-17T11:00:00Z"));

        QualifiedName mainActivityId = cPF.newCpmQualifiedName("mainActivity");
        Activity mainActivity = cPF.getProvFactory().newActivity(mainActivityId);
        mainActivity.setStartTime(pF.newISOTime("2025-08-16T10:00:00Z"));
        mainActivity.setEndTime(pF.newISOTime("2025-08-17T11:00:00Z"));

        QualifiedName backwardConnector1Id = cPF.newCpmQualifiedName("backwardConnector1");
        Entity backwardConnector1 = cPF.getProvFactory().newEntity(backwardConnector1Id);
        backwardConnector1.getType().add(pF.newType(
                new org.openprovenance.prov.vanilla.QualifiedName(cpmUri, "backwardConnector", "cpm"),
                pF.getName().PROV_QUALIFIED_NAME
        ));

        QualifiedName backwardConnector1SpecId = cPF.newCpmQualifiedName("backwardConnector1Spec");
        Entity backwardConnector1Spec = cPF.getProvFactory().newEntity(backwardConnector1SpecId);

        QualifiedName forwardConnector1Id = cPF.newCpmQualifiedName("forwardConnector1");
        Entity forwardConnector1 = cPF.getProvFactory().newEntity(forwardConnector1Id);

        QualifiedName forwardConnector1SpecId = cPF.newCpmQualifiedName("forwardConnector1Spec");
        Entity forwardConnector1Spec = cPF.getProvFactory().newEntity(forwardConnector1SpecId);

        *//*Relation relation = cPF.getProvFactory().newWasAttributedTo(cPF.newCpmQualifiedName("attr"), entityId, senderAgentId);
        Relation relation2 = cPF.getProvFactory().newWasAssociatedWith(cPF.newCpmQualifiedName("assoc"), activityId, senderAgentId);
        Relation relation3 = cPF.getProvFactory().newWasGeneratedBy(cPF.newCpmQualifiedName("gen"), entityId, activityId);
        *//*

        Relation specRelation1 = cPF.getProvFactory().newSpecializationOf(entity1Id, backwardConnector1Id);
        Relation specRelation2 = cPF.getProvFactory().newSpecializationOf(entity3Id, forwardConnector1Id);
        Relation specRelation3 = cPF.getProvFactory().newSpecializationOf(backwardConnector1SpecId, backwardConnector1Id);
        Relation specRelation4 = cPF.getProvFactory().newSpecializationOf(forwardConnector1SpecId, forwardConnector1Id);

        Relation usedRelation1 = cPF.getProvFactory().newUsed(cPF.newCpmQualifiedName("used1"), entity1Id, activity1Id);
        Relation usedRelation2 = cPF.getProvFactory().newUsed(cPF.newCpmQualifiedName("used2"), entity2Id, activity2Id);
        Relation usedRelation3 = cPF.getProvFactory().newUsed(cPF.newCpmQualifiedName("used3"), backwardConnector1Id, mainActivityId);

        Relation genRelation1 = cPF.getProvFactory().newWasGeneratedBy(cPF.newCpmQualifiedName("gen1"), entity2Id, activity1Id);
        Relation genRelation2 = cPF.getProvFactory().newWasGeneratedBy(cPF.newCpmQualifiedName("gen2"), entity3Id, activity2Id);
        Relation genRelation3 = cPF.getProvFactory().newWasGeneratedBy(cPF.newCpmQualifiedName("gen3"), forwardConnector1Id, mainActivityId);

        Relation derRelation2 = cPF.getProvFactory().newWasDerivedFrom(cPF.newCpmQualifiedName("der1"), entity3Id, entity2Id);
        Relation derRelation3 = cPF.getProvFactory().newWasDerivedFrom(cPF.newCpmQualifiedName("der2"), entity2Id, entity1Id);
        Relation derRelation1 = cPF.getProvFactory().newWasDerivedFrom(cPF.newCpmQualifiedName("der3"), forwardConnector1Id, backwardConnector1Id);

        QualifiedName bundleId = pF.newQualifiedName("www.example.com/", "bundle3", "ex");

        CpmDocument cpmDocument = new CpmDocument(List.of(
                entity1,
                entity2,
                entity3,
                activity1,
                activity2,
                mainActivity,
                backwardConnector1,
                backwardConnector1Spec,
                forwardConnector1,
                forwardConnector1Spec,
                specRelation1,
                specRelation2,
                specRelation3,
                specRelation4,
                usedRelation1,
                usedRelation2,
                usedRelation3,
                genRelation1,
                genRelation2,
                genRelation3,
                derRelation1,
                derRelation2,
                derRelation3
        ), bundleId, pF, cPF, cF);
        return cpmDocument.toDocument();
    }*/

    public QualifiedName getBundleId(Document document) {
        Bundle bundle = (Bundle) document.getStatementOrBundle().get(0);
        return bundle.getId();
    }

    public static String getExtension(Formats.ProvFormat format) {
        return switch (format) {
            case RDFXML -> ".xml";
            case JSON -> ".json";
            case JSONLD -> ".jsonld";
            case PROVN -> ".provn";
            case TURTLE -> ".ttl";
            case TRIG -> ".trig";
            default -> ".txt";
        };
    }

    public String serialize(Document document, Formats.ProvFormat format) {
        var filePath = dataFolder + getBundleId(document).getLocalPart() + "_" + format.toString() + getExtension(format);
        var interop = new InteropFramework();
        interop.writeDocument(filePath, document, format);
        return filePath;
    }

    public Document deserialize(String filePath, Formats.ProvFormat format) throws IOException {
        var interop = new InteropFramework();
        InputStream inputStream = new FileInputStream(filePath);
        return interop.readDocument(inputStream, format);
    }

    public CpmDocument testFormat(Document document, Formats.ProvFormat format) throws IOException {
        String filePath = serialize(document, format);
        Document deserializedDocument = deserialize(filePath, format);
        return new CpmDocument(deserializedDocument, pF, cPF, cF);
    }

    @Test
    public void testMultipleFormats() throws IOException, DatatypeConfigurationException {
        Document[] documents = {
                getTestDocument1(),
                getTestDocument2(),
        };

        Formats.ProvFormat[] formats = {
                Formats.ProvFormat.JSON,
                Formats.ProvFormat.JSONLD,
                Formats.ProvFormat.PROVN,
        };

        boolean allTestsPassed = true;

        for (Document document : documents) {
            for (Formats.ProvFormat format : formats) {
                try {
                    testFormat(document, format);
                } catch (Exception e) {
                    System.err.println("Error testing format: " + format +
                            " for document with bundle ID: " + getBundleId(document).getLocalPart());
                    e.printStackTrace();
                    allTestsPassed = false;
                }
            }
        }

        assert (allTestsPassed);

    }
}
