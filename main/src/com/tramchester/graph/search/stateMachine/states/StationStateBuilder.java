package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalDate;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;

public abstract class StationStateBuilder {

    protected Stream<Relationship> addValidDiversions(Node node, Iterable<Relationship> relationships, TraversalState traversalState) {
        return addValidDiversions(node, Streams.stream(relationships), traversalState);
    }


    public Stream<Relationship> addValidDiversions(Node node, Stream<Relationship> relationships, TraversalState traversalState) {
        if (!node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            return relationships;
        }
        LocalDate queryDate = traversalState.traversalOps.getQueryDate();
        Stream<Relationship> diversions = Streams.stream(node.getRelationships(Direction.OUTGOING, DIVERSION));
        Stream<Relationship> validOnDate = diversions.
                filter(relationship -> !GraphProps.getStartDate(relationship).until(queryDate).isNegative()).
                filter(relationship -> !queryDate.until(GraphProps.getEndDate(relationship)).isNegative());
        return Stream.concat(validOnDate, relationships);
    }
}
