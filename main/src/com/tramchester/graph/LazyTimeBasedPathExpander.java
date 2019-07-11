package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class LazyTimeBasedPathExpander implements PathExpander<Double> {
    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpander.class);

    private final RelationshipFactory relationshipFactory;
    private final ServiceHeuristics serviceHeuristics;

//    private final Map<Node, Integer> visited;

    public LazyTimeBasedPathExpander(RelationshipFactory relationshipFactory, ServiceHeuristics serviceHeuristics) {
        this.relationshipFactory = relationshipFactory;
        this.serviceHeuristics = serviceHeuristics;
//        visited = new HashMap<>();
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Double> branchState) {
        return () -> new RelationshipIterable(path);
    }

    public void reportVisits(int threshhold) {
//        visited.entrySet().stream().
//                filter(entry -> (entry.getValue()>=threshhold)).
//                forEach(entry -> logger.warn(format("Node %s %s count was %s",
//                        entry.getKey().getProperty(GraphStaticKeys.ID),entry.getKey().getLabels(),entry.getValue())));
    }

    public class RelationshipIterable implements Iterator<Relationship> {
        private final Path path;
        private final Iterator<Relationship> relationships;
        private final boolean justBoarded;

        private Relationship next;

        public RelationshipIterable(Path path) {
            this.path = path;
            this.relationships = path.endNode().getRelationships(OUTGOING).iterator();

            Relationship inboundToLastNode = path.lastRelationship();
            if (inboundToLastNode!=null) {
                justBoarded = inboundToLastNode.isType(BOARD) || inboundToLastNode.isType(INTERCHANGE_BOARD);
            } else {
                justBoarded = false;
            }
        }

        @Override
        public boolean hasNext() {
            while (relationships.hasNext()) {
                next = relationships.next();
                if (next.isType(TRAM_GOES_TO)) {
                    if (interestedIn(next)) {
                        return true;
                    }
                } else {
                    if (!justBoarded) {
                        return true;
                    }
                    // so we just boarded a tram, don't attempt to immediately get off again
                    boolean departing = next.isType(DEPART) || next.isType(INTERCHANGE_DEPART);
                    if (!departing) {
                        return true;
                    }
                    // not interested
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("Skipping relationship %s %s->%s with properties %s",
                                next.getId(), next.getStartNodeId(), next.getEndNodeId(), next.getAllProperties()));
                    }
                }

            }
            return false;
        }

        @Override
        public Relationship next() {
            return next;
        }

        private boolean interestedIn(Relationship graphRelationship) {

            TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;

            try {
                TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
                ServiceReason serviceReason = serviceHeuristics.checkServiceHeuristics(incoming, goesToRelationship, path);
                return serviceReason.isValid();
            } catch (TramchesterException e) {
                logger.error("Unable to check service heuristics",e);
            }
            return false;
        }
    }

    @Override
    public PathExpander<Double> reverse() {
        return this;
    }

}

