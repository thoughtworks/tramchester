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

    protected Stream<Relationship> addValidDiversions(Node node, Iterable<Relationship> relationships,
                                                      TraversalState traversalState, boolean alreadyOnDiversion) {
        return addValidDiversions(node, Streams.stream(relationships), traversalState, alreadyOnDiversion);
    }


    public Stream<Relationship> addValidDiversions(Node node, Stream<Relationship> relationships,
                                                   TraversalState traversalState, boolean alreadyOnDiversion) {

        if ((!alreadyOnDiversion) && node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            LocalDate queryDate = traversalState.traversalOps.getQueryDate();
            Stream<Relationship> diversions = Streams.stream(node.getRelationships(Direction.OUTGOING, DIVERSION));
            Stream<Relationship> validOnDate = diversions.filter(relationship -> GraphProps.validOn(queryDate, relationship));
            return Stream.concat(validOnDate, relationships);
        }

        return relationships;
    }


}
