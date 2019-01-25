package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class LazyTimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpander.class);

    private RelationshipFactory relationshipFactory;
    private ServiceHeuristics serviceHeuristics;
    private boolean edgePerService;

    public LazyTimeBasedPathExpander(RelationshipFactory relationshipFactory, ServiceHeuristics serviceHeuristics,
                                     TramchesterConfig config) {
        this.relationshipFactory = relationshipFactory;
        this.serviceHeuristics = serviceHeuristics;
        this.edgePerService = config.getEdgePerTrip();
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> branchState) {
        Node endNode = path.endNode();

        if (edgePerService) {
            // only pursue outbound edges from a service if days and date match for the service node
            if (endNode.hasLabel(TransportGraphBuilder.Labels.SERVICE)) {
                if (serviceHeuristics.checkServiceHeuristics(endNode) != ServiceReason.IsValid) {
                    //logger.info("Skipping service node " + endNode.getProperty(ID));
                    return new LinkedList<>();
                }
            }
        }

        return () -> new RelationshipIterable(path);
    }

    public class RelationshipIterable implements Iterator<Relationship> {
        private final Path path;
        private final Iterator<Relationship> relationships;
        private final Relationship inboundToLastNode;
        private Relationship next;

        public RelationshipIterable(Path path) {
            this.path = path;
            this.relationships = path.endNode().getRelationships(Direction.OUTGOING).iterator();
            inboundToLastNode = path.lastRelationship();
        }

        @Override
        public boolean hasNext() {
            while (relationships.hasNext()) {
                next = relationships.next();
//                    if (path.length()==0) {
//                        return true;
//                    }
                if (edgePerService && next.isType(SERVICE)) {
                    // follow an edge to service node only if service id matches
                    if (inboundToLastNode.isType(TRAM_GOES_TO)) {
                        String svcId = next.getProperty(SERVICE_ID).toString();
                        if (svcId.equals(inboundToLastNode.getProperty(SERVICE_ID))) {
                            return true;
                        }
                    } else if (inboundToLastNode.isType(BOARD) || inboundToLastNode.isType(INTERCHANGE_BOARD)) {
                        return true;
                    }
                } else {
                    if (!next.isType(TRAM_GOES_TO)) {
                        return true;
                    }
                    if (interestedIn(next, path)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Relationship next() {
            return next;
        }
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

