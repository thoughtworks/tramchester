package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import org.joda.time.LocalDate;
import org.neo4j.graphalgo.CostEvaluator;
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

        Node endNode = path.endNode();
        TramNode currentNode = nodeFactory.getNode(endNode);

        Iterable<Relationship> relationships = endNode.getRelationships(Direction.OUTGOING);

        if (path.length()==0) { // start of journey
            return relationships;
        }

        TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
        Set<Relationship> results = new HashSet<>();

        List<ServiceReason> servicesFilteredOut = new LinkedList<>();
        int servicesOutbound = 0;

        for (Relationship graphRelationship : relationships) {
            TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            if (outgoing.isGoesTo()) {
                // filter route station -> route station relationships
                GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;
                servicesOutbound++;
                ServiceReason serviceReason = null;
                try {
                    serviceReason = checkServiceHeuristics(state, incoming,
                            goesToRelationship, path);
                } catch (TramchesterException e) {
                    logger.error("Unable to check service heuristics",e);
                }
                if (serviceReason==ServiceReason.IsValid) {
                    results.add(graphRelationship);
                } else {
                    servicesFilteredOut.add(serviceReason);
                }
            }
            else {
                // just add the relationship
                results.add(graphRelationship);
            }
        }

        // all filtered out
        if ((servicesOutbound>0) && (servicesFilteredOut.size()==servicesOutbound)) {
            reportFilterReasons(currentNode, servicesFilteredOut, incoming);
        }

        return results;
    }

    private void reportFilterReasons(TramNode currentNode,
                                     List<ServiceReason> servicesFilteredOut,
                                     TransportRelationship incoming) {
        if (servicesFilteredOut.size()==0) {
            return;
        }
        logger.info(format("Filtered:%s all services for node:%s inbound:%s",
                servicesFilteredOut.size(), currentNode, incoming));
//        if (logger.isDebugEnabled()) {
//            StringBuilder output = new StringBuilder();
//            servicesFilteredOut.forEach(reason -> output.append(reason).append(" "));
//            logger.debug(output.toString());
//        }
    }

    private ServiceReason checkServiceHeuristics(BranchState<GraphBranchState> branchState, TransportRelationship incoming,
                                                 GoesToRelationship tramGoesToRelationship, Path path) throws TramchesterException {
        if (!operatesOnDayOnWeekday(tramGoesToRelationship.getDaysTramRuns(),  branchState.getState().getDay())) {
            return new ServiceReason.DoesNotRunOnDay(branchState.getState().getDay());
        }
        if (!noInFlightChangeOfService(incoming, tramGoesToRelationship)) {
            return ServiceReason.InflightChangeOfService;
        }
        if (!operatesOnQueryDate(tramGoesToRelationship.getStartDate(), tramGoesToRelationship.getEndDate(),
                branchState.getState().getQueryDate()))
        {
            return ServiceReason.DoesNotRunOnQueryDate;
        }
        // do this last, it is expensive
        ProvidesElapsedTime elapsedTimeProvider = new ProvidesElapsedTime(path, branchState, costEvaluator);
        if (!operatesOnTime(tramGoesToRelationship.getTimesTramRuns(), elapsedTimeProvider)) {
            return new ServiceReason.DoesNotOperateOnTime(elapsedTimeProvider.getElapsedTime());
        }
        return ServiceReason.IsValid;
    }

    private boolean operatesOnQueryDate(TramServiceDate startDate, TramServiceDate endDate, TramServiceDate queryDate) {
        // start date and end date are inclusive
        LocalDate date = queryDate.getDate();
        LocalDate startingDate = startDate.getDate();
        LocalDate endingDate = endDate.getDate();
        if  (date.isAfter(startingDate) && date.isBefore(endingDate)) {
            return true;
        }
        if (date.equals(startingDate) || date.equals(endingDate)) {
            return true;
        }
        return false;
    }

    public boolean noInFlightChangeOfService(TransportRelationship incoming, GoesToRelationship outgoing) {
        if (!incoming.isGoesTo()) {
            return true; // not a connecting relationship
        }
        GoesToRelationship goesToRelationship = (GoesToRelationship) incoming;
        String service = goesToRelationship.getService();
        return service.equals(outgoing.getService());
    }

    public boolean operatesOnTime(int[] times, ElapsedTime provider) throws TramchesterException {
        if (times.length==0) {
            logger.warn("No times provided");
        }
        int elapsedTime = provider.getElapsedTime();
        // the times array is sorted in ascending order
        for (int nextTram : times) {
            if ((nextTram - elapsedTime) > maxWaitMinutes) {
                return false; // array sorted, so no need to go further
            }

            if (nextTram>=elapsedTime) { // check next tram not in the past
                if ((nextTram-elapsedTime) <= maxWaitMinutes) {
                    if (provider.startNotSet()) {
                        int realJounrneyStartTime = nextTram-TransportGraphBuilder.BOARDING_COST;
                        provider.setJourneyStart(realJounrneyStartTime);
                    }
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

