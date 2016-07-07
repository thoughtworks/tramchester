package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.joda.time.LocalDate;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ServiceHeuristics {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);

    private CostEvaluator<Double> costEvaluator;
    private int maxWaitMinutes;

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, TramchesterConfig config) {
        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
    }

    public ServiceReason checkServiceHeuristics(BranchState<GraphBranchState> branchState, TransportRelationship incoming,
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

    public boolean operatesOnQueryDate(TramServiceDate startDate, TramServiceDate endDate, TramServiceDate queryDate) {
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
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("Tram operates on time. Times: '%s' ElapsedTime '%s'", log(times), provider));
                    }
                    return true;  // within max wait time
                }
            }
        }
        return false;
    }

    private String log(int[] times) {
        StringBuilder builder = new StringBuilder();
        for (int time : times) {
            builder.append(time+" ");
        }
        return builder.toString();
    }
}
