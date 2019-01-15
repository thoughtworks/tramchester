package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Optional;

public class ServiceHeuristics implements PersistsBoardingTime {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final TramServiceDate date;
    private final DaysOfWeek day;
    private final LocalTime queryTime;

    private CostEvaluator<Double> costEvaluator;
    private Optional<LocalTime> boardingTime;
    private int maxWaitMinutes;

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, TramchesterConfig config, TramServiceDate date,
                             LocalTime queryTime) {
        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.config = config;
        this.date = date;
        this.day = date.getDay();
        this.queryTime = queryTime;
        boardingTime = Optional.empty();
    }

    public ServiceReason checkServiceHeuristics(TransportRelationship incoming,
                                                GoesToRelationship goesToRelationship, Path path) throws TramchesterException {

        if (!operatesOnDayOnWeekday(goesToRelationship.getDaysServiceRuns(), day)) {
            return new ServiceReason.DoesNotRunOnDay(day);
        }
        if (!noInFlightChangeOfService(incoming, goesToRelationship)) {
            return ServiceReason.InflightChangeOfService;
        }
        if (!operatesOnQueryDate(goesToRelationship.getStartDate(), goesToRelationship.getEndDate(), date))
        {
            return ServiceReason.DoesNotRunOnQueryDate;
        }

        if (config.getEdgePerTrip()) {
            ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
            // single time per edge
            LocalTime time = goesToRelationship.getTimeServiceRuns();
            if (!operatesOnTime(time, elapsedTimeProvider)) {
                return new ServiceReason.DoesNotOperateOnTime(elapsedTimeProvider.getElapsedTime());
            }
        } else {
            ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
            // all times for the service per edge
            if (!operatesOnTime(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
                return new ServiceReason.DoesNotOperateOnTime(elapsedTimeProvider.getElapsedTime());
            }
        }

        return ServiceReason.IsValid;
    }

    public boolean operatesOnQueryDate(TramServiceDate startDate, TramServiceDate endDate, TramServiceDate queryDate) {
        return Service.operatesOn(startDate, endDate, queryDate.getDate());
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

    // todo retire this version
    public boolean operatesOnTime(LocalTime[] times, ElapsedTime provider) throws TramchesterException {
        if (times.length==0) {
            logger.warn("No times provided");
        }
        LocalTime journeyClockTime = provider.getElapsedTime();
        TramTime journeyClock = TramTime.of(journeyClockTime);

        // the times array is sorted in ascending order
        for (LocalTime nextTramTime : times) {
            TramTime nextTram = TramTime.of(nextTramTime);

            // if wait until this tram is too long can stop now
            if (nextTramTime.isAfter(journeyClockTime) &&
                    TramTime.diffenceAsMinutes(nextTram, journeyClock)>maxWaitMinutes) {
                return false;
            }

            if (nextTram.departsAfter(journeyClock)) {
                if (TramTime.diffenceAsMinutes(nextTram, journeyClock) <= maxWaitMinutes) {
                    if (provider.startNotSet()) {
                        LocalTime realJounrneyStartTime = nextTramTime.minusMinutes(TransportGraphBuilder.BOARDING_COST);
                        provider.setJourneyStart(realJounrneyStartTime);
                    }
//                    if (logger.isDebugEnabled()) {
//                        logger.debug(format("Tram operates on time. Times: '%s' ElapsedTime '%s'", log(times), provider));
//                    }
                    return true;  // within max wait time
                }
            }
        }
        return false;
    }

    public boolean operatesOnTime(LocalTime time, ElapsedTime provider) throws TramchesterException {
        LocalTime journeyClockTime = provider.getElapsedTime();
        TramTime journeyClock = TramTime.of(journeyClockTime);

        // the times array is sorted in ascending order
        TramTime nextTram = TramTime.of(time);

        // if wait until this tram is too long can stop now
        if (time.isAfter(journeyClockTime) &&
                TramTime.diffenceAsMinutes(nextTram, journeyClock)>maxWaitMinutes) {
            return false;
        }

        if (time.isAfter(journeyClockTime)) {
            if (TramTime.diffenceAsMinutes(nextTram, journeyClock) <= maxWaitMinutes) {
                if (provider.startNotSet()) {
                    LocalTime realJounrneyStartTime = time.minusMinutes(TransportGraphBuilder.BOARDING_COST);
                    provider.setJourneyStart(realJounrneyStartTime);
                }
                return true;  // within max wait time
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

    @Override
    public void save(LocalTime time) {
        boardingTime = Optional.of(time);
    }

    @Override
    public boolean isPresent() {
        return boardingTime.isPresent();
    }

    @Override
    public LocalTime get() {
        return boardingTime.get();
    }

    @Override
    public void clear() {
        boardingTime = Optional.empty();
    }
}
