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

public class PlatformState extends TraversalState {
    private final long platformNodeId;

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    public PlatformState(TraversalState parent, Iterable<Relationship> relationships, long platformNodeId, int cost) {
        super(parent, relationships, cost);
        this.platformNodeId = platformNodeId;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {

        long nodeId = node.getId();

        if (nodeLabel == GraphBuilder.Labels.TRAM_STATION) {
            if (nodeId ==destinationNodeId) {
                return new DestinationState(this, cost);
            }
            return new TramStationState(this,
                    filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), platformNodeId),
                    cost, nodeId);
        }

        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            try {
                journeyState.boardTram();
            } catch (TramchesterException e) {
                throw new RuntimeException("unable to board tram", e);
            }
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM), platformNodeId);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationState(this, outbounds, nodeId, cost, true);
        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }
}
