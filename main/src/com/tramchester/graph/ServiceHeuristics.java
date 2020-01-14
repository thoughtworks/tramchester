package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.RunningServices;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.tramchester.graph.GraphStaticKeys.ID;

public class ServiceHeuristics implements PersistsBoardingTime, BasicServiceHeuristics {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private static final boolean debugEnabled = logger.isDebugEnabled();

    private final TramchesterConfig config;
    private final RunningServices runningServices;
    private final String endStationId;
    private final TramTime queryTime;
    private final ServiceReasons reasons;
    private final ReachabilityRepository reachabilityRepository;

    private final CostEvaluator<Double> costEvaluator;
    private final CachedNodeOperations nodeOperations;
    private final int maxJourneyDuration;

    private Optional<TramTime> boardingTime;
    private final int maxWaitMinutes;

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, CachedNodeOperations nodeOperations,
                             ReachabilityRepository reachabilityRepository, TramchesterConfig config, TramTime queryTime,
                             RunningServices runningServices, String endStationId, ServiceReasons reasons) {
        this.nodeOperations = nodeOperations;
        this.reachabilityRepository = reachabilityRepository;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.maxJourneyDuration = config.getMaxJourneyDuration();
        this.queryTime = queryTime;
        this.runningServices = runningServices;
        this.endStationId = endStationId;
        this.reasons = reasons;

        // for none edge per trip path
        boardingTime = Optional.empty();

    }
    
    // edge per trip
    public ServiceReason checkServiceDate(Node node, Path path) {
        reasons.incrementTotalChecked();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.isRunning(nodeServiceId)) {
            return valid(path);
        }

        return notOnQueryDate(path, nodeServiceId);
    }

    public ServiceReason checkServiceTime(Path path, Node node, TramTime currentClock) {
        reasons.incrementTotalChecked();

        String serviceId = nodeOperations.getServiceId(node);

        // prepared to wait up to max wait for start of a service...
        TramTime serviceStart = runningServices.getServiceEarliest(serviceId).minusMinutes(maxWaitMinutes);

        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = runningServices.getServiceLatest(serviceId);

        if (!currentClock.between(serviceStart, serviceEnd)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, path));
        }

        return valid(path);
    }

    // edge per trip
    public ServiceReason checkTime(Path path, Node node, TramTime currentElapsed) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentElapsed, path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return valid(path);
        }
        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, path));
    }

    private boolean operatesWithinTime(TramTime nodeTime, TramTime elapsedTimed) {
        TramTime earliest = nodeTime.minusMinutes(maxWaitMinutes);
        //TramTime clock = TramTime.of(elapsedTimed);

        return elapsedTimed.between(earliest, nodeTime);
    }

    public ServiceReason checkServiceHeuristics(TransportRelationship incoming,
                                                GoesToRelationship goesToRelationship, Path path) throws TramchesterException {
        if (config.getEdgePerTrip()) {
            throw new RuntimeException("Should not call this for edgePerTrip");
        }

        reasons.incrementTotalChecked();
        // already checked via service node for edge per trip
        String serviceId = goesToRelationship.getServiceId();
        if (!runningServices.isRunning(serviceId)) {
            return notOnQueryDate(path, serviceId);
        }

        ServiceReason inflightChange = sameService(path, incoming, goesToRelationship);
        if (!inflightChange.isValid()) {
            return reasons.recordReason(inflightChange);
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!underMaxWait(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(elapsedTimeProvider.getElapsedTime(),
                    path));
        }

        return valid(path);
    }

    public ServiceReason sameService(Path path, TransportRelationship transportRelationship, GoesToRelationship outgoing) {
        reasons.incrementTotalChecked();

        if (!transportRelationship.isGoesTo()) {
            return valid(path); // not a connecting/goes to relationship, no svc id
        }

        GoesToRelationship incoming = (GoesToRelationship) transportRelationship;
        String service = incoming.getServiceId();

        if (service.equals(outgoing.getServiceId())) {
            return valid(path);
        }

        return reasons.recordReason(ServiceReason.InflightChangeOfService(service, path));
    }

    public boolean underMaxWait(TramTime[] times, ElapsedTime provider) throws TramchesterException {
        if (times.length==0) {
            logger.warn("No times provided");
        }
        reasons.incrementTotalChecked();
        TramTime journeyClockTime = provider.getElapsedTime();

        // the times array is sorted in ascending order
        for (TramTime nextTram : times) {
            //TramTime nextTram = TramTime.of(nextTramTime);

            // if wait until this tram is too long can stop now
            int diffenceAsMinutes = TramTime.diffenceAsMinutes(nextTram, journeyClockTime);

            if (nextTram.isAfterBasic(journeyClockTime) && diffenceAsMinutes > maxWaitMinutes) {
                return false;
            }

            if (nextTram.departsAfter(journeyClockTime)) {
                if (diffenceAsMinutes <= maxWaitMinutes) {
                    if (provider.startNotSet()) {
                        TramTime realJounrneyStartTime = nextTram.minusMinutes(TransportGraphBuilder.BOARDING_COST);
                        provider.setJourneyStart(realJounrneyStartTime);
                    }
                    return true;  // within max wait time
                }
            }
        }
        return false;
    }

    @Override
    public void save(TramTime time) {
        boardingTime = Optional.of(time);
    }

    @Override
    public boolean isPresent() {
        return boardingTime.isPresent();
    }

    @Override
    public TramTime get() {
        return boardingTime.get();
    }

    public ServiceReason interestedInHour(Path path, int hour, TramTime journeyClockTime) {
        reasons.incrementTotalChecked();

        int queryTimeHour = journeyClockTime.getHourOfDay();
        if (hour == queryTimeHour) {
            // quick win
            return valid(path);
        }

        // this only works if maxWaitMinutes<60
        int previousHour = hour - 1;
        if (previousHour==-1) {
            previousHour = 23;
        }
        if (queryTimeHour == previousHour) {
            int timeUntilNextHour = 60 - maxWaitMinutes;
            if (journeyClockTime.getMinuteOfHour() >= timeUntilNextHour) {
                return valid(path);
            }
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, path));
    }


    public boolean toEndStation(Relationship depart) {
        return depart.getProperty(GraphStaticKeys.STATION_ID).toString().equals(endStationId);
    }

    public ServiceReason canReachDestination(Node endNode, Path path) {
        String stationId = endNode.getProperty(ID).toString();
        boolean flag = reachabilityRepository.reachable(stationId, endStationId);
        if (flag) {
            return valid(path);
        }
        return reasons.recordReason(ServiceReason.StationNotReachable(path));
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final Path path) {
        if (totalCost>maxJourneyDuration) {
            return reasons.recordReason(ServiceReason.TookTooLong(queryTime.plusMinutes(totalCost), path));
        }

        return valid(path);
    }

    private ServiceReason valid(final Path path) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.IsValid(path));
        }
        return reasons.recordReason(ServiceReason.isValid);
    }

    private ServiceReason notOnQueryDate(Path path, String nodeServiceId) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));
        }
        return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

}
