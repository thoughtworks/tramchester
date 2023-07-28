package com.tramchester.graph.search.stateMachine;

import com.google.common.collect.Streams;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.states.RouteStationState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public abstract class TowardsRouteStation<T extends RouteStationState> implements Towards<T> {

    private final boolean interchangesOnly;

    public TowardsRouteStation(boolean interchangesOnly) {
        this.interchangesOnly = interchangesOnly;
    }

    protected OptionalResourceIterator<Relationship> getTowardsDestination(TraversalOps traversalOps, Node node, TramDate date) {
        Stream<Relationship> relationships = Streams.stream(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART, DIVERSION_DEPART));
        return traversalOps.getTowardsDestination(Stream.concat(relationships, getActiveDiversions(node,date).stream()));
    }

    // TODO When to follow diversion departs? Should these be (also) INTERCHANGE_DEPART ?
    protected Stream<Relationship> getOutboundsToFollow(Node node, boolean isInterchange, TramDate date) {
        Stream<Relationship> outboundsToFollow = Stream.empty();
        if (interchangesOnly) {
            if (isInterchange) {
                outboundsToFollow = Streams.stream(node.getRelationships(OUTGOING, INTERCHANGE_DEPART));
            }
        } else {
            outboundsToFollow = Streams.stream(node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART));
        }

        List<Relationship> diversions = getActiveDiversions(node, date);
        if (diversions.isEmpty()) {
            return outboundsToFollow;
        } else {
            return Stream.concat(outboundsToFollow, diversions.stream());
        }
    }

    private List<Relationship> getActiveDiversions(Node node, TramDate date) {
        Set<Relationship> diversions = Streams.
                stream(node.getRelationships(OUTGOING, DIVERSION_DEPART)).
                collect(Collectors.toSet());
        List<Relationship> collect = diversions.stream().
                filter(relationship -> GraphProps.validOn(date, relationship)).
                collect(Collectors.toList());
        return collect;
    }


}
