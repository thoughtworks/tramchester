package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DIVERSION;

public abstract class StationStateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StationStateBuilder.class);

    protected Stream<Relationship> addValidDiversions(Node node, Iterable<Relationship> relationships,
                                                      TraversalState traversalState, boolean alreadyOnDiversion) {
        return addValidDiversions(node, Streams.stream(relationships), traversalState, alreadyOnDiversion);
    }


    public Stream<Relationship> addValidDiversions(Node node, Stream<Relationship> relationships,
                                                   TraversalState traversalState, boolean alreadyOnDiversion) {

        if (alreadyOnDiversion) {
            logger.info("Already on diversion " + GraphProps.getStationId(node));
            return relationships;
        }

        if (node.hasRelationship(Direction.OUTGOING, DIVERSION)) {
            TramDate queryDate = traversalState.traversalOps.getQueryDate();
            Stream<Relationship> diversions = Streams.stream(node.getRelationships(Direction.OUTGOING, DIVERSION));
            Stream<Relationship> validOnDate = diversions.filter(relationship -> GraphProps.validOn(queryDate, relationship));
            return Stream.concat(validOnDate, relationships);
        }

        return relationships;
    }


}
