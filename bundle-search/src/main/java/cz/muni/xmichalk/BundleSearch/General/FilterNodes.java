package cz.muni.xmichalk.BundleSearch.General;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.IEdge;
import cz.muni.fi.cpm.model.INode;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.DTO.QualifiedNameDTO;
import cz.muni.xmichalk.Util.ProvDocumentUtils;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.model.interop.Formats;
import org.openprovenance.prov.vanilla.ProvFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static cz.muni.xmichalk.Util.ProvDocumentUtils.deserialize;

public final class FilterNodes {
    
    public List<INode> apply(CpmDocument document, QualifiedName startNodeId, Predicate<INode> filter) {
        Set<INode> visited = new HashSet<>();
        Queue<INode> queue = new ArrayDeque<>();
        List<INode> results = new ArrayList<>();
        
        INode start = document.getNode(startNodeId);
        if (start == null) {
            // TODO throw an error or return a message?
            // or do queue.addAll(doc.getNodes()); and continue?
            return null;
        }

        queue.add(start);

        while (!queue.isEmpty()) {
            INode current = queue.poll();
            if (current == null || visited.contains(current)) continue;

            visited.add(current);

            if (filter.test(current)) {
                results.add(current);
            }

            for (IEdge e : current.getCauseEdges()) {
                queue.add(e.getEffect());
            }
            for (IEdge e : current.getEffectEdges()) {
                queue.add(e.getCause());
            }
        }
        
        return results;
    }
}
