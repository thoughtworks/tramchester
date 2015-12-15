package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TramRelationship;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

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

        Node endNode = path.endNode();
        TramNode currentNode = nodeFactory.getNode(endNode);

        Iterable<Relationship> relationships = endNode.getRelationships(Direction.OUTGOING);

        if (path.length()==0) { // start of journey
            return relationships;
        }

        TramRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());

        Set<Relationship> results = new HashSet<>();

        int duration = (int)new WeightedPathImpl(costEvaluator, path).weight();
        int journeyStartTime = branchState.getTime();
        int elapsedTime = duration + journeyStartTime;
        List<ServiceReason> servicesFilteredOut = new LinkedList<>();
        int servicesOutbound = 0;

        for (Relationship graphRelationship : relationships) {
            TramRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            if (outgoing.isTramGoesTo()) {
                // filter route station -> route station relationships
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) outgoing;
                servicesOutbound++;
                ServiceReason serviceReason = checkServiceHeuristics(branchState, incoming, elapsedTime,
                        tramGoesToRelationship);
                if (serviceReason==ServiceReason.IsValid) {
                    results.add(graphRelationship);
                } else {
                    servicesFilteredOut.add(serviceReason);
                }
            } else  {
                results.add(graphRelationship);
            }
        }

//         all filtered out
        if ((servicesOutbound>0) && (servicesFilteredOut.size()==servicesOutbound)) {
            //logger.info("All services filtered out " + currentNode);
            reportFilterReasons(currentNode, elapsedTime, servicesFilteredOut, incoming);
        }

        return results;
    }

    private void reportFilterReasons(TramNode currentNode, int elapsedTime,
                                     List<ServiceReason> servicesFilteredOut,
                                     TramRelationship incoming) {
        if (servicesFilteredOut.size()==0) {
            return;
        }
        logger.info(format("Time:%s Filtered:%s services for node:%s inbound:%s",
                elapsedTime, servicesFilteredOut.size(), currentNode, incoming));
        StringBuilder output = new StringBuilder();
        servicesFilteredOut.forEach(reason -> output.append(reason).append(" "));
        logger.debug(output.toString());
    }

    private ServiceReason checkServiceHeuristics(GraphBranchState branchState, TramRelationship incoming,
                                                 int elapsedTime, TramGoesToRelationship tramGoesToRelationship) {
        if (!operatesOnDayOnWeekday(tramGoesToRelationship.getDaysTramRuns(), branchState.getDay())) {
            return ServiceReason.DoesNotRunOnDay;
        }
        if (!noInFlightChangeOfService(incoming, tramGoesToRelationship)) {
            return ServiceReason.InflightChangeOfService;
        }
        if (!operatesOnTime(tramGoesToRelationship.getTimesTramRuns(), elapsedTime)) {
            return ServiceReason.DoesNotOperateOnTime;
        }
        if (!operatesOnQueryDate(tramGoesToRelationship.getStartDate(), tramGoesToRelationship.getEndDate(), branchState.getQueryDate()))
        {
            return ServiceReason.DoesNotRunOnQueryDate;
        }
        return ServiceReason.IsValid;
    }

    private boolean operatesOnQueryDate(TramServiceDate startDate, TramServiceDate endDate, TramServiceDate queryDate) {
        // start date and end date are inclusive
        LocalDate date = queryDate.getDate();
        if  (date.isAfter(startDate.getDate()) && date.isBefore(endDate.getDate())) {
            return true;
        }
        if (date.equals(startDate) || date.equals(endDate)) {
            return true;
        }
        return false;
    }

    public boolean noInFlightChangeOfService(TramRelationship incoming, TramGoesToRelationship outgoing) {
        if (!incoming.isTramGoesTo()) {
            return true; // not a tram service relationship
        }
        TramGoesToRelationship inComingTram = (TramGoesToRelationship) incoming;
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

    public boolean operatesOnDayOnWeekday(boolean[] days, DaysOfWeek today) {
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

        return operates;
    }

    @Override
    public PathExpander<GraphBranchState> reverse() {
        return this;
    }

}

