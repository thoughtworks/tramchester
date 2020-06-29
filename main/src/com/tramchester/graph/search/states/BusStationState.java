package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.search.JourneyState;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collection;
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
            return toRouteStation(node, journeyState, cost, nodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }

    @NotNull
    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost, long nodeId) {
        try {
            journeyState.boardBus();
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board bus", e);
        }
        List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                DEPART, INTERCHANGE_DEPART), stationNodeId);

        outbounds.addAll(orderSvcRelationships(node));
        //orderSvcRelationships(node).forEach(outbounds::add);

        return new RouteStationState(this, outbounds, nodeId, cost, true);
    }

    private Collection<Relationship> orderSvcRelationships(Node node) {
        Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

        List<SortsPositions.HasStationId<Relationship>> relationships = new ArrayList<>();
        toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));

        return sortsPositions.sortedByNearTo(destinationStationIds, relationships);
    }

    @Override
    public String toString() {
        return "BusStationState{" +
                "stationNodeId=" + stationNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;

        private RelationshipFacade(Relationship relationship) {
            this.relationship = relationship;
        }

        @Override
        public String getStationId() {
            return relationship.getProperty(GraphStaticKeys.TOWARDS_STATION_ID).toString();
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}
