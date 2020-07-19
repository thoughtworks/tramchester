package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ServiceReason;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PreviousSuccessfulVisits {
    private final ConcurrentMap<Long, TramTime> hourVisits;
    private final Set<Long> timeVisits;

    private final NodeTypeRepository nodeTypeRepository;

    public PreviousSuccessfulVisits(NodeTypeRepository nodeTypeRepository) {
        this.nodeTypeRepository = nodeTypeRepository;
        hourVisits = new ConcurrentHashMap<>();
        timeVisits = ConcurrentHashMap.newKeySet();
    }

    public void clear() {
        timeVisits.clear();
        hourVisits.clear();
    }

    public boolean hasUsableResult(Node node, TramTime journeyClock) {
        // NOTE: We only cache previous for certain node types: Time and Hour
        // as we can *only* safely exclude previous nodes if there is only one outbound path and hence no variation
        // in results

        long nodeId = node.getId();

        if (timeVisits.contains(nodeId)) {
            // Time nodes encode a specific time, so the previous time *must* match for this node id
            return true;
        }

        TramTime previousTime = hourVisits.getOrDefault(nodeId, null);

        if (previousTime==null) {
            return false;
        }

        return previousTime.equals(journeyClock); // been here before at exact same time, so no need to continue
    }

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Node endNode, TramTime journeyClock) {
        switch (result) {
            case NotAtQueryTime:
            case TimeOk:
                timeVisits.add(endNode.getId());
                break;
            case HourOk:
                hourVisits.putIfAbsent(endNode.getId(), journeyClock);
                break;
        }
    }
}
