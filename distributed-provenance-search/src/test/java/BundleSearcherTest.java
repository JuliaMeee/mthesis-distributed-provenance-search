import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearcher.BreadthFirstBundleSearcher;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.vanilla.ProvFactory;


import cz.muni.fi.cpm.model.INode;

import java.util.List;

public class BundleSearcherTest {
    ProvFactory pF = new ProvFactory();
    ICpmFactory cF = new CpmMergedFactory(pF);
    ICpmProvFactory cPF = new CpmProvFactory(pF);
    ProvUtilities u = new ProvUtilities();
    String dataFolder = System.getProperty("user.dir") + "/src/test/resources/data/";

    private CpmDocument getSimpleDocument() {
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

        QualifiedName bundleId = pF.newQualifiedName("uri", "bundle", "ex");

        return new CpmDocument(List.of(entity, agent, activity, relation, relation2, relation3), bundleId, pF, cPF, cF);
    }

    @Test
    public void testInvalidStartNodeId() {
        var doc = getSimpleDocument();
        QualifiedName invalidStartNodeId = cPF.newCpmQualifiedName("invalidId");

        var searcher = new BreadthFirstBundleSearcher();
        List<INode> results = searcher.search(doc, invalidStartNodeId, node -> true);

        assert results.isEmpty();
    }

    @Test
    public void testSearchById() {
        var doc = getSimpleDocument();
        QualifiedName startNodeId = cPF.newCpmQualifiedName("activity1");
        QualifiedName targetNodeId = cPF.newCpmQualifiedName("entity1");
        INode targetNode = doc.getNode(targetNodeId);

        var searcher = new BreadthFirstBundleSearcher();
        List<INode> results = searcher.search(doc, startNodeId, node -> node.getId().equals(targetNodeId));

        assert results.size() == 1 && results.contains(targetNode);
    }
}
