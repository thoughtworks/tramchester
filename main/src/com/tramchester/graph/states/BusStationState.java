package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class BusStationState extends TraversalState {
    private final long stationNodeId;

    public BusStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        long nodeId = node.getId();
        if (nodeId == destinationNodeId) {
            // TODO Cost of bus depart?
            return new DestinationState(this, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.QUERY_NODE) {
            return new WalkingState(this, node.getRelationships(OUTGOING), cost);
        }
        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            try {
                journeyState.boardBus();
            } catch (TramchesterException e) {
                throw new RuntimeException("unable to board tram", e);
            }
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                    DEPART, INTERCHANGE_DEPART), stationNodeId);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationState(this, outbounds, nodeId, cost, true);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }

    @Override
    public String toString() {
        return "BusStationState{" +
                "stationNodeId=" + stationNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }
}
