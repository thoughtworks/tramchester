package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ServiceReason;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreviousSuccessfulVisits {
    private final Map<Long, TramTime> hourVisits;
    private final Set<Long> timeVisits;

    private final NodeTypeRepository nodeTypeRepository;

    public PreviousSuccessfulVisits(NodeTypeRepository nodeTypeRepository) {
        this.nodeTypeRepository = nodeTypeRepository;
        hourVisits = new HashMap<>();
        timeVisits = new HashSet<>();
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
        if (hourVisits.containsKey(nodeId)) {
            TramTime previousVisitTime = hourVisits.get(nodeId);
            if (previousVisitTime.equals(journeyClock)) {
                return true; // been here before at exact same time, so no need to continue
            }
        }

        return false;
    }

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Node endNode, TramTime journeyClock) {
        if (result ==  ServiceReason.ReasonCode.NotAtQueryTime) {
            timeVisits.add(endNode.getId());
        }
        if (result == ServiceReason.ReasonCode.Valid) {
            if (nodeTypeRepository.isTime(endNode))  {
                timeVisits.add(endNode.getId());
            } else if (nodeTypeRepository.isHour(endNode)) {
                hourVisits.put(endNode.getId(), journeyClock);
            }
        }
    }
}
