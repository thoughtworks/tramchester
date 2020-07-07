package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.Map;

public class PreviousSuccessfulVisits {
    private final Map<Long, TramTime> visits;
    private final NodeTypeRepository nodeTypeRepository;

    public PreviousSuccessfulVisits(NodeTypeRepository nodeTypeRepository) {
        this.nodeTypeRepository = nodeTypeRepository;
        visits= new HashMap<>();
    }

    public void clear() {
        visits.clear();
    }

    private boolean seenBefore(long nodeId) {
        return visits.containsKey(nodeId);
    }

    private TramTime getTimeOfVisit(long nodeId) {
        return visits.get(nodeId);
    }

    private void recordVisit(long nodeId, TramTime time) {
        visits.put(nodeId, time);
    }

    public boolean hasUsableResult(Node endNode, TramTime journeyClock) {
        long endNodeId = endNode.getId();

        if (seenBefore(endNodeId)) {
            // NOTE: We only cache previous for certain node types: Time and Hour
            // can *only* safely exclude previous nodes if there is only one outbound path

            if (nodeTypeRepository.isTime(endNode)) {
                // Time nodes encode a specific time, so the previous time *must* match for this node id
                return true;
            }

            TramTime previousVisitTime = getTimeOfVisit(endNodeId);
            if (previousVisitTime.equals(journeyClock) && nodeTypeRepository.isHour(endNode)) {
                return true; // been here before at exact same time, so no need to continue
            }
        }
        return false;
    }

    public void recordVisitIfUseful(Node endNode, TramTime journeyClock) {
        if (nodeTypeRepository.isTime(endNode) || nodeTypeRepository.isHour(endNode) ) {
            recordVisit(endNode.getId(), journeyClock);
        }
    }
}
