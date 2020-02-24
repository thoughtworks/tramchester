package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
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
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {

        if (nodeLabel == TransportGraphBuilder.Labels.TRAM_STATION || nodeLabel == TransportGraphBuilder.Labels.BUS_STATION) {
            if (node.getId()==destinationNodeId) {
                return new DestinationState(this, cost);
            }
            return new StationState(this,
                    filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), platformNodeId),
                    cost, node.getId());
        }

        if (nodeLabel == TransportGraphBuilder.Labels.ROUTE_STATION) {
            try {
                journeyState.boardTram();
            } catch (TramchesterException e) {
                throw new RuntimeException("unable to board tram", e);
            }
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM), platformNodeId);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationState(this, outbounds, node.getId(), cost, true);
        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }
}
