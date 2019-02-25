package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class LazyTimeBasedPathExpander implements PathExpander<Double> {
    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpander.class);

    private final LocalTime queryTime;
    private final RelationshipFactory relationshipFactory;
    private final ServiceHeuristics serviceHeuristics;

    private final boolean edgePerService;
    private final NodeOperations nodeOperations;
    private final CostEvaluator<Double> cachingCostEvaluator;

    public LazyTimeBasedPathExpander(LocalTime queryTime, RelationshipFactory relationshipFactory, ServiceHeuristics serviceHeuristics,
                                     TramchesterConfig config, NodeOperations nodeOperations, CostEvaluator<Double> cachingCostEvaluator) {
        this.queryTime = queryTime;
        this.relationshipFactory = relationshipFactory;
        this.serviceHeuristics = serviceHeuristics;
        edgePerService = config.getEdgePerTrip();
        this.nodeOperations = nodeOperations;
        this.cachingCostEvaluator = cachingCostEvaluator;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Double> branchState) {
        if (!edgePerService) {
            return () -> new RelationshipIterable(path);
        }

        Node endNode = path.endNode();
        // not same as branchState.getState() as accounts for actual board time which != query time
        LocalTime currentElapsed = calculateElapsedTimeForPath(path);

        // only pursue outbound edges from a service service runs today & within time
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkService(endNode, currentElapsed).isValid()) {
                return Collections.emptyList();
            }
        }

        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(hour, currentElapsed).isValid()) {
                return Collections.emptyList();
            }
        }

        // only follow hour nodes if match up with possible journeys
        if (nodeOperations.isMinute(endNode)) {
            if (!serviceHeuristics.checkTime(endNode, currentElapsed).isValid()) {
                return Collections.emptyList();
            }
        }

        return buildSimpleExpandsionList(path);
    }

    private Iterable<Relationship> buildSimpleExpandsionList(Path path) {
        LinkedList<Relationship> result = new LinkedList<>();
        Relationship inboundToLastNode = path.lastRelationship();

        if (inboundToLastNode==null) {
            return path.endNode().getRelationships(OUTGOING);
        }

        boolean inboundWasGoesTo = inboundToLastNode.isType(TRAM_GOES_TO);
        boolean inboundWasBoarding = inboundToLastNode.isType(BOARD) || inboundToLastNode.isType(INTERCHANGE_BOARD);
        String inboundSvcId = inboundWasGoesTo ? inboundToLastNode.getProperty(SERVICE_ID).toString() : "";

        Iterable<Relationship> iter = path.endNode().getRelationships(OUTGOING);
        iter.forEach(relationship -> {
            if (relationship.isType(TO_SERVICE)) {
                if (serviceHeuristics.checkForSvcChange(relationship, inboundWasGoesTo, inboundWasBoarding, inboundSvcId)) {
                    result.add(relationship);
                }
            } else {
                if (inboundWasBoarding) {
                    if (!(relationship.isType(DEPART) || relationship.isType(INTERCHANGE_DEPART))) {
                        // don't allow getting on then just getting off again
                        result.addLast(relationship);
                    }
                } else {
                    result.addLast(relationship);
                }
            }
        });

        return result;
    }

    public LocalTime calculateElapsedTimeForPath(Path path) {
        LocalTime time = backtrackToLastSeenTime(path, queryTime);
        return calculateCostToStartOrLastTimeSeen(path, time);
    }

    public LocalTime calculateCostToStartOrLastTimeSeen(Path path, LocalTime time) {
        int cost = 0;
        Iterator<Relationship> relationshipIterator = path.reverseRelationships().iterator();
        while(relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();
            cost = cost +  cachingCostEvaluator.getCost(relationship, OUTGOING).intValue();
            if (relationship.isType(TRAM_GOES_TO)) {
                return time.plusMinutes(cost);
            }
        }
        return time.plusMinutes(cost);
    }

    public LocalTime backtrackToLastSeenTime(Path path, LocalTime time) {
        Iterator<Node> nodes = path.reverseNodes().iterator();
        // skip first one, will be current time node
        nodes.next();
        while(nodes.hasNext()) {
            Node node = nodes.next();
            if (nodeOperations.isMinute(node)) {
                return nodeOperations.getTime(node);
            }
        }
        return time;
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
                }
            }
            return false;
        }

        @Override
        public Relationship next() {
            return next;
        }

        private boolean interestedIn(Relationship graphRelationship) {
            // NOT called for edgePerService

            TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;

            try {
                TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
                ServiceReason serviceReason = serviceHeuristics.checkServiceHeuristics(incoming, goesToRelationship, path);
                return serviceReason==ServiceReason.IsValid;
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

