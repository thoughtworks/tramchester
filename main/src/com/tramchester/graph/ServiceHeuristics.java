package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;

public class ServiceHeuristics implements PersistsBoardingTime {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final Set<String> runningServices;
    private final LocalTime queryTime;
    private final List<ServiceReason> reasons;

    private final CostEvaluator<Double> costEvaluator;
    private final NodeOperations nodeOperations;
    private Optional<LocalTime> boardingTime;
    private final int maxWaitMinutes;

    // stats
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final AtomicInteger inflightChange = new AtomicInteger(0);
    private final AtomicInteger dateWrong = new AtomicInteger(0);
    private final AtomicInteger timeWrong = new AtomicInteger(0);

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, NodeOperations nodeOperations, TramchesterConfig config,
                             LocalTime queryTime, Set<String> runningServices) {
        this.nodeOperations = nodeOperations;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.queryTime = queryTime;
        this.runningServices = runningServices;

        // for none edge per trip path
        boardingTime = Optional.empty();

        // diagnostics, needs debug
        reasons = new LinkedList<>();
    }
    
    // edge per trip
    // TODO change to TramTime
    public ServiceReason checkService(Node node, LocalTime currentElapsed){
        totalChecked.incrementAndGet();
        // date

        String nodeServiceId = nodeOperations.getServiceId(node);
        if (!runningServices.contains(nodeServiceId)) {
            dateWrong.incrementAndGet();
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId));
        }

        // prepared to wait up to max wait for start of a service...
        LocalTime serviceStart = nodeOperations.getServiceEarliest(node).asLocalTime().minusMinutes(maxWaitMinutes);
        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = nodeOperations.getServiceLatest(node);

        TramTime currentClock = TramTime.of(currentElapsed);
        if (!currentClock.between(TramTime.of(serviceStart), serviceEnd)) {
            timeWrong.getAndIncrement();
            return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, "ServiceNotRunning:"+nodeServiceId));
        }

        return ServiceReason.IsValid;
    }

    // edge per trip
    public ServiceReason checkTime(Node node, LocalTime currentElapsed) {
        totalChecked.getAndIncrement();

        LocalTime nodeTime = nodeOperations.getTime(node);
        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return ServiceReason.IsValid;
        }
        timeWrong.incrementAndGet();
        return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, "TimeMistmact:"+currentElapsed.toString()));
    }

    private boolean operatesWithinTime(LocalTime nodeTime, LocalTime elapsedTimed) {
        TramTime earliest = TramTime.of(nodeTime.minusMinutes(maxWaitMinutes));
        TramTime clock = TramTime.of(elapsedTimed);

        return clock.between(earliest, TramTime.of(nodeTime));
    }

    public ServiceReason checkServiceHeuristics(TransportRelationship incoming,
                                                GoesToRelationship goesToRelationship, Path path) throws TramchesterException {

        if (config.getEdgePerTrip()) {
            // if node based time check is working should not need to actually check edges by this point
            return ServiceReason.IsValid;
        }

        totalChecked.incrementAndGet();
        // already checked via service node for edge per trip
        String serviceId = goesToRelationship.getServiceId();
        if (!runningServices.contains(serviceId)) {
            dateWrong.incrementAndGet();
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(serviceId));
        }

        if (!sameService(incoming, goesToRelationship).isValid()) {
            return recordReason(ServiceReason.InflightChangeOfService);
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!operatesOnTime(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            timeWrong.incrementAndGet();
            return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime,
                    elapsedTimeProvider.getElapsedTime().toString()));
        }

        return ServiceReason.IsValid;
    }

    // caller records
    public ServiceReason sameService(TransportRelationship incoming, GoesToRelationship outgoing) {
        if (!incoming.isGoesTo()) {
            return ServiceReason.IsValid; // not a connecting/goes to relationship, no svc id
        }

        GoesToRelationship goesToRelationship = (GoesToRelationship) incoming;
        String service = goesToRelationship.getServiceId();
        if (service.equals(outgoing.getServiceId())) {
            return ServiceReason.IsValid;
        }

        inflightChange.incrementAndGet();
        return ServiceReason.InflightChangeOfService;
    }

    public boolean operatesOnTime(LocalTime[] times, ElapsedTime provider) throws TramchesterException {
        if (times.length==0) {
            logger.warn("No times provided");
        }
        totalChecked.getAndIncrement();
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
                    return true;  // within max wait time
                }
            }
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
        logger.info("Service change: " + inflightChange.get());
        logger.info("Time wrong: " + timeWrong.get());
    }

    public ServiceReason interestedInHour(int hour, LocalTime journeyClockTime) {
        // quick win
        totalChecked.getAndIncrement();

        int queryTimeHour = queryTime.getHour();
        if (hour== queryTimeHour) {
            return ServiceReason.IsValid;
        }

        TramTime latestTimeInHour = TramTime.of(hour, 59);

        TramTime earliestTimeInHour = TramTime.of(LocalTime.of(hour,0).minusMinutes(maxWaitMinutes));
        TramTime earliestTime = TramTime.of(journeyClockTime);

        if (earliestTime.between(earliestTimeInHour,latestTimeInHour)) {
            return ServiceReason.IsValid;
        }

        timeWrong.getAndIncrement();
        return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, earliestTimeInHour.toString()));
    }

    public boolean checkForSvcChange(Relationship next, boolean inboundWasGoesTo, boolean inboundWasBoarding, String inboundSvcId) {
        // only called if next is a Service relationship

        if (inboundWasGoesTo) {
            // can't magically jump between trams without getting off first
            String svcId = next.getProperty(SERVICE_ID).toString();
            if (svcId.equals(inboundSvcId)) {
                // same service
                return true;
            } else {
                inflightChange.incrementAndGet();
                return false;
            }
        }
        // else
        if (inboundWasBoarding) {
            // just got on board, so don't care about previous service
            return true;
        }
        throw new RuntimeException("should only reach route station node via boarding or from a goes to link");
        // else
        //return false;
    }

    public void reportReasons() {
        reportStats();
        if (logger.isDebugEnabled()) {
            reasons.forEach(reason -> logger.debug("ServiceReason: " + reason ));
        }
    }

    // TODO counters into here
    private ServiceReason recordReason(ServiceReason serviceReason) {
        if (logger.isDebugEnabled()) {
            reasons.add(serviceReason);
        }
        return serviceReason;
    }
}
