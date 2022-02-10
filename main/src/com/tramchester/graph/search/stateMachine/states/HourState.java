package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {

    public static class Builder implements Towards<HourState> {

        private final boolean depthFirst;
        private final NodeContentsRepository nodeContents;

        public Builder(boolean depthFirst, NodeContentsRepository nodeContents) {
            this.depthFirst = depthFirst;
            this.nodeContents = nodeContents;
        }

        public HourState fromService(ServiceState serviceState, Node node, Duration cost, ExistingTrip maybeExistingTrip) {
            Stream<Relationship> relationships = getMinuteRelationships(node);
            return new HourState(serviceState, relationships, maybeExistingTrip, cost);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(ServiceState.class, this);
        }

        @Override
        public Class<HourState> getDestination() {
            return HourState.class;
        }

        Stream<Relationship> getMinuteRelationships(Node node) {
            Stream<Relationship> relationships = Streams.stream(node.getRelationships(OUTGOING, TO_MINUTE));
            if (depthFirst) {
                //return relationships.sorted(TramTime.comparing(GraphProps::getTime));
                return relationships.
                        sorted(TramTime.comparing(relationship -> nodeContents.getTime(relationship.getEndNode())));
            }
            return relationships;
        }
    }

    private final ExistingTrip maybeExistingTrip;

    private HourState(TraversalState parent, Stream<Relationship> relationships,
                      ExistingTrip maybeExistingTrip, Duration cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    protected TraversalState toMinute(MinuteState.Builder towardsMinute, Node minuteNode, Duration cost, JourneyStateUpdate journeyState) {
        try {
            TramTime time = traversalOps.getTimeFrom(minuteNode);
            journeyState.recordTime(time, getTotalDuration());
        } catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }

        return towardsMinute.fromHour(this, minuteNode, cost, maybeExistingTrip, journeyState);
    }

    @Override
    public String toString() {
        return "HourState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }
}
