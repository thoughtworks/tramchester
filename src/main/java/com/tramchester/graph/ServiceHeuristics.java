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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;

public class ServiceHeuristics implements PersistsBoardingTime {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final TramServiceDate date;
    private final DaysOfWeek day;
    private final LocalTime queryTime;

    private final CostEvaluator<Double> costEvaluator;
    private final NodeOperations nodeOperations;
    private Optional<LocalTime> boardingTime;
    private final int maxWaitMinutes;

    // stats
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final AtomicInteger inflightChange = new AtomicInteger(0);
    private final AtomicInteger dateWrong = new AtomicInteger(0);
    private final AtomicInteger timeWrong = new AtomicInteger(0);
    private final AtomicInteger dayWrong = new AtomicInteger(0);

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, NodeOperations nodeOperations, TramchesterConfig config, TramServiceDate date,
                             LocalTime queryTime) {
        this.nodeOperations = nodeOperations;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.date = date;
        this.day = date.getDay();
        this.queryTime = queryTime;
        boardingTime = Optional.empty();
    }
    
    // edge per trip
    public ServiceReason checkService(Node node){
        totalChecked.incrementAndGet();
        // days
        boolean[] days = nodeOperations.getDays(node);
        if (!operatesOnDayOnWeekday(days, day)) {
            dayWrong.incrementAndGet();
            return new ServiceReason.DoesNotRunOnDay(day);
        }
        // date
        TramServiceDate startDate = nodeOperations.getServiceStartDate(node);
        TramServiceDate endDate = nodeOperations.getServiceEndDate(node);
        if (!operatesOnQueryDate(startDate, endDate, date)) {
            dateWrong.incrementAndGet();
            return ServiceReason.DoesNotRunOnQueryDate;
        }
        return ServiceReason.IsValid;
    }


    // edge per trip
    public ServiceReason checkTime(LocalTime nodeTime, LocalTime currentElapsed) {

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return ServiceReason.IsValid;
        }
        timeWrong.incrementAndGet();
        return new ServiceReason.DoesNotOperateOnTime(queryTime);
    }

    public ServiceReason checkServiceHeuristics(TransportRelationship incoming,
                                                GoesToRelationship goesToRelationship, Path path) throws TramchesterException {

        totalChecked.incrementAndGet();

        if (!config.getEdgePerTrip()) {
            // already checked via service node for edge per trip
            if (!operatesOnDayOnWeekday(goesToRelationship.getDaysServiceRuns(), day)) {
                dayWrong.incrementAndGet();
                return new ServiceReason.DoesNotRunOnDay(day);
            }
            if (!operatesOnQueryDate(goesToRelationship.getStartDate(), goesToRelationship.getEndDate(), date))
            {
                dateWrong.incrementAndGet();
                return ServiceReason.DoesNotRunOnQueryDate;
            }
            if (!sameService(incoming, goesToRelationship)) {
                inflightChange.incrementAndGet();
                return ServiceReason.InflightChangeOfService;
            }
        }

        if (config.getEdgePerTrip()) {
            // if node based time check is working should not need to actually check edges by this point
            return ServiceReason.IsValid;

        } else {
            ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
            // all times for the service per edge
            if (!operatesOnTime(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
                timeWrong.incrementAndGet();
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

    public boolean sameService(TransportRelationship incoming, GoesToRelationship outgoing) {
        if (!incoming.isGoesTo()) {
            return true; // not a connecting relationship
        }
        GoesToRelationship goesToRelationship = (GoesToRelationship) incoming;
        String service = goesToRelationship.getService();
        return service.equals(outgoing.getService());
    }

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
            int diffenceAsMinutes = TramTime.diffenceAsMinutes(nextTram, journeyClock);

            if (nextTramTime.isAfter(journeyClockTime) && diffenceAsMinutes > maxWaitMinutes) {
                return false;
            }

            if (nextTram.departsAfter(journeyClock)) {
                if (diffenceAsMinutes <= maxWaitMinutes) {
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

    private boolean operatesWithinTime(LocalTime timeHere, LocalTime journeyClockTime) {
        TramTime journeyClock = TramTime.of(journeyClockTime);
        if (timeHere.isAfter(journeyClockTime) || timeHere.equals(journeyClockTime)) {
            TramTime nextTram = TramTime.of(timeHere);

            int diffenceAsMinutes = TramTime.diffenceAsMinutes(nextTram, journeyClock);

            if (diffenceAsMinutes > maxWaitMinutes) {
                return false;
            }
            return true;  // within max wait time
        }
        return false;
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

    public void reportStats() {
        logger.info("Total checked: " + totalChecked.get());
        logger.info("Date mismatch: " + dateWrong.get());
        logger.info("Day mismatch: " + dayWrong.get());
        logger.info("Service change: " + inflightChange.get());
        logger.info("Time wrong: " + timeWrong.get());
    }

    // querytime + costSoFar + maxWait (for board) = latest time could arrive here
    // querytime + costSoFar + 0 = earlier time could arrive here
    public boolean interestedInHour(int hour, int costSoFar) {
        // quick win
        int queryTimeHour = queryTime.getHour();
        if (hour== queryTimeHour) {
            return true;
        }
        TramTime earliestTime = TramTime.of(queryTime.plusMinutes(costSoFar));


        TramTime latestTimeInHour = TramTime.of(hour, 59);
        if (latestTimeInHour.compareTo(earliestTime)<0) {
            return false;
        }
//        if (TramTime.diffenceAsMinutes(earliestTime, latestTimeInHour)<60) {
//            return true;
//        }

        TramTime earliestTimeInHour = TramTime.of(hour, 0);
        if (TramTime.diffenceAsMinutes(earliestTimeInHour, earliestTime)<=maxWaitMinutes) {
            return true;
        }

        return false;
    }

    public boolean checkForSvcChange(boolean inboundWasGoesTo, boolean inboundWasBoarding, String inboundSvcId,
                                     Relationship next) {

        // ONLY follow an edge to service node only if service id matches
        if (inboundWasGoesTo) {
            String svcId = next.getProperty(SERVICE_ID).toString();
            if (svcId.equals(inboundSvcId)) {
                // same service
                return true;
            }
        }
        // else
        if (inboundWasBoarding) {
            // just got on board, so don't care about previous service id
            return true;
        }
        // else
        inflightChange.incrementAndGet();
        return false;
    }
}
