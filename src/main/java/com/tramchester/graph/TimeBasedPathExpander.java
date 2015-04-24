package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private static final Logger logger = LoggerFactory.getLogger(TimeBasedPathExpander.class);

    private final NodeFactory nodeFactory;
    private CostEvaluator<Double> costEvaluator;
    private int maxWaitMinutes;
    private RelationshipFactory relationshipFactory;

    public TimeBasedPathExpander(CostEvaluator<Double> costEvaluator, int maxWaitMinutes,
                                 RelationshipFactory relationshipFactory,
                                 NodeFactory nodeFactory) {
        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = maxWaitMinutes;
        this.relationshipFactory = relationshipFactory;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> state) {
        GraphBranchState branchState = state.getState();

        TramNode currentNode = nodeFactory.getNode(path.endNode());
        Set<Relationship> relationships = currentNode.getRelationships();

        if (path.length()==0) {
            return relationships;
        }

        TramRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
        List<Relationship> results = new ArrayList<>();

        int duration = (int)new WeightedPathImpl(costEvaluator, path).weight();
        int journeyStartTime = branchState.getTime();
        int elapsedTime = duration + journeyStartTime;
        List<GoesToRelationship> servicesFilteredOut = new ArrayList<>();
        int servicesOutbound = 0;

        for (Relationship graphRelationship : relationships) {
            TramRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            if (outgoing.isGoesTo()) {
                GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;
                servicesOutbound++;
                // filter route station -> route station relationships
                if (operatesOnTime(goesToRelationship.getTimesTramRuns(), elapsedTime) &&
                        operatesOnDay(goesToRelationship.getDaysTramRuns(), branchState.getDay()) &&
                        noInFlightChangeOfService(incoming, goesToRelationship)
                        ) {
                    results.add(graphRelationship);
                } else {
                    servicesFilteredOut.add(goesToRelationship);
                }
            } else if (outgoing.isInterchange()) {
                // add interchange relationships
                results.add(graphRelationship);
            } else {
                // add board and depart
                results.add(graphRelationship);
            }
        }

        if (duration>90) {
            logger.warn("Duration >90mins at node " + currentNode);
        }
        if ((servicesOutbound>0) && (servicesFilteredOut.size()==servicesOutbound)) {
            logger.warn(String.format("Filtered out all %s services for node %s time %s ", servicesFilteredOut.size(), currentNode, elapsedTime));
//            logger.debug("Filtered out services were: " + servicesFilteredOut);
        }
        return results;
    }

    public boolean noInFlightChangeOfService(TramRelationship incoming, GoesToRelationship outgoing) {
        if (!incoming.isGoesTo()) {
            return true; // not a tram service relationship
        }
        GoesToRelationship inComingTram = (GoesToRelationship) incoming;
        return inComingTram.getService().equals(outgoing.getService());
    }

    public boolean operatesOnTime(int[] times, int currentTime) {
        // the times array is sorted in ascending order
        for (int nextTram : times) {
            if ((nextTram - currentTime) > maxWaitMinutes) {
                return false; // array sorted, so no need to go further
            }

            if (nextTram>=currentTime) { // check next tram not in the past
                if ((nextTram-currentTime) <= maxWaitMinutes) {
                    return true;  // within max wait time
                }
            }
        }
        return false;
    }

    public boolean operatesOnDay(boolean[] days, DaysOfWeek today) {
        boolean operates = false;
        switch (today) {
            case Monday:
                operates = days[0];
                break;
            case Tuesday:
                operates = days[1];
                break;
            case Wednesday:
                operates =  days[2];
                break;
            case Thursday:
                operates = days[3];
                break;
            case Friday:
                operates = days[4];
                break;
            case Saturday:
                operates = days[5];
                break;
            case Sunday:
                operates = days[6];
                break;

        }
//        if (!operates) {
//            logger.info("Service does not run on " + today);
//        }
        return operates;
    }

    @Override
    public PathExpander<GraphBranchState> reverse() {
        return this;
    }

}

