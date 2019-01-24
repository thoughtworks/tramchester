package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.TRAM_GOES_TO;

public class LazyTimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpander.class);

    private RelationshipFactory relationshipFactory;
    private ServiceHeuristics serviceHeuristics;

    public LazyTimeBasedPathExpander(RelationshipFactory relationshipFactory, ServiceHeuristics serviceHeuristics) {
        this.relationshipFactory = relationshipFactory;
        this.serviceHeuristics = serviceHeuristics;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> branchState) {

        Node endNode = path.endNode();
        Iterable<Relationship> relationships = endNode.getRelationships(Direction.OUTGOING);

        Iterable<Relationship> results = () -> new Iterator<Relationship>() {
            private Relationship next;
            private Iterator<Relationship> relationshipIterator = relationships.iterator();
            @Override
            public boolean hasNext() {
                while (relationshipIterator.hasNext()) {
                    next = relationshipIterator.next();
                    if (path.length()==0) {
                        return true;
                    }
                    if (!next.isType(TRAM_GOES_TO)) {
                        return true;
                    }
                    if (interestedIn(next, path)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Relationship next() {
                return next;
            }
        };

        return results;
    }

    private boolean interestedIn(Relationship graphRelationship, Path path) {
        TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
        TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);

        GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;

        try {
            ServiceReason serviceReason = serviceHeuristics.checkServiceHeuristics(incoming, goesToRelationship, path);
            return serviceReason==ServiceReason.IsValid;
        } catch (TramchesterException e) {
            logger.error("Unable to check service heuristics",e);
        }
        return false;
    }

    @Override
    public PathExpander<GraphBranchState> reverse() {
        return this;
    }

}

