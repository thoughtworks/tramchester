package com.tramchester.graph.search.states;

import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateJustBoarded extends TraversalState {

    public static class Builder {
        private final SortsPositions sortsPositions;
        private final List<String> destinationStationIds;

        public Builder(SortsPositions sortsPositions, List<String> destinationStationIds) {
            this.sortsPositions = sortsPositions;
            this.destinationStationIds = destinationStationIds;
        }

        public TraversalState fromPlatformState(PlatformState platformState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM), platformState);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationStateJustBoarded(platformState, outbounds, cost);
        }

        public TraversalState fromBusStation(BusStationState busStationState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                    DEPART, INTERCHANGE_DEPART), busStationState);
            outbounds.addAll(orderSvcRelationships(node));
            return new RouteStationStateJustBoarded(busStationState, outbounds, cost);
        }

        private Collection<Relationship> orderSvcRelationships(Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

            List<SortsPositions.HasStationId<Relationship>> relationships = new ArrayList<>();
            toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));

            return sortsPositions.sortedByNearTo(destinationStationIds, relationships);
        }
    }

    private final ServiceState.Builder serviceStateBuilder;

    private RouteStationStateJustBoarded(TraversalState traversalState, List<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
        serviceStateBuilder = new ServiceState.Builder();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return serviceStateBuilder.fromRouteStation(this, nextNode, cost);
        }

        // if one to one relationship between platforms and route stations, or bus stations and route stations,
        // no longer holds then this will throw
        throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
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
