import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.template.mapper.ITemplateProvMapper;
import cz.muni.fi.cpm.template.mapper.TemplateProvMapper;
import cz.muni.fi.cpm.template.schema.*;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.vanilla.ProvFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;
import java.util.Map;

public class TestDocument {
    public static Document getTestDocument1(ProvFactory pF, ICpmProvFactory cPF, ICpmFactory cF) {
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

    public static Document getTestDocument2(ProvFactory pF, ICpmProvFactory cPF, ICpmFactory cF) {
        try {
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
