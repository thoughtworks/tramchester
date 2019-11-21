package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.ReachabilityRepository;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.*;
import static java.lang.String.format;

public class ServiceHeuristics implements PersistsBoardingTime {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final Set<String> runningServices;
    private final Set<String> preferRoutes;
    private final String endStationId;
    private final LocalTime queryTime;
    private final List<ServiceReason> reasons;
    private final ReachabilityRepository reachabilityRepository;

    private final CostEvaluator<Double> costEvaluator;
    private final CachedNodeOperations nodeOperations;
    private final int maxJourneyDuration;

    private Optional<LocalTime> boardingTime;
    private final int maxWaitMinutes;
//    private final int maxJourneyMins = 170; // longest end to end is 163?

    // stats
    private final Map<ServiceReason.ReasonCode,AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, CachedNodeOperations nodeOperations,
                             ReachabilityRepository reachabilityRepository, TramchesterConfig config, LocalTime queryTime, Set<String> runningServices,
                             Set<String> preferRoutes,
                             String endStationId) {
        this.nodeOperations = nodeOperations;
        this.reachabilityRepository = reachabilityRepository;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.maxJourneyDuration = config.getMaxJourneyDuration();
        this.queryTime = queryTime;
        this.runningServices = runningServices;
        this.preferRoutes = preferRoutes;
        this.endStationId = endStationId;

        // for none edge per trip path
        boardingTime = Optional.empty();

        // diagnostics, needs debug
        reasons = new LinkedList<>();

        statistics = new HashMap<>();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }
    
    // edge per trip
    // TODO change to TramTime
    public ServiceReason checkServiceDate(Node node, Path path) {
        totalChecked.incrementAndGet();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.contains(nodeServiceId)) {
            return recordReason(ServiceReason.IsValid(path,"dateDay"));
        }

        return recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));

    }

    private void incrementStat(ServiceReason.ReasonCode reasonCode) {
        statistics.get(reasonCode).incrementAndGet();
    }

    public ServiceReason checkServiceTime(Path path, Node node, LocalTime currentElapsed) {
        totalChecked.incrementAndGet();

        // prepared to wait up to max wait for start of a service...
        LocalTime serviceStart = nodeOperations.getServiceEarliest(node).asLocalTime().minusMinutes(maxWaitMinutes);
        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = nodeOperations.getServiceLatest(node);

        TramTime currentClock = TramTime.of(currentElapsed);
        if (!currentClock.between(TramTime.of(serviceStart), serviceEnd)) {
            String nodeServiceId = nodeOperations.getServiceId(node);
            return recordReason(ServiceReason.ServiceNotRunningAtTime(currentElapsed, "ServiceNotRunning:"+nodeServiceId,
                    path));
        }

        return recordReason(ServiceReason.IsValid(path,"svcTimes"));
    }

    // edge per trip
    public ServiceReason checkTime(Path path, Node node, LocalTime currentElapsed) {
        totalChecked.getAndIncrement();

        LocalTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, currentElapsed.toString(), path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return recordReason(ServiceReason.IsValid(path, "timeNode"));
        }
        return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, currentElapsed.toString(), path));
    }

    private boolean operatesWithinTime(LocalTime nodeTime, LocalTime elapsedTimed) {
        TramTime earliest = TramTime.of(nodeTime.minusMinutes(maxWaitMinutes));
        TramTime clock = TramTime.of(elapsedTimed);

        return clock.between(earliest, TramTime.of(nodeTime));
    }

    public ServiceReason checkServiceHeuristics(TransportRelationship incoming,
                                                GoesToRelationship goesToRelationship, Path path) throws TramchesterException {

        if (config.getEdgePerTrip()) {
            throw new RuntimeException("Should not call this for edgePerTrip");
        }

        totalChecked.incrementAndGet();
        // already checked via service node for edge per trip
        String serviceId = goesToRelationship.getServiceId();
        if (!runningServices.contains(serviceId)) {
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(serviceId, path));
        }

        ServiceReason inflightChange = sameService(path, incoming, goesToRelationship);
        if (!inflightChange.isValid()) {
            return recordReason(inflightChange);
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!operatesOnTime(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime,
                    elapsedTimeProvider.getElapsedTime().toString(), path));
        }

        return recordReason(ServiceReason.IsValid(path,"ok"));
    }

    public ServiceReason sameService(Path path, TransportRelationship transportRelationship, GoesToRelationship outgoing) {
        if (!transportRelationship.isGoesTo()) {
            return recordReason(ServiceReason.IsValid(path,"notGoesTo")); // not a connecting/goes to relationship, no svc id
        }

        GoesToRelationship incoming = (GoesToRelationship) transportRelationship;
        String service = incoming.getServiceId();

        if (service.equals(outgoing.getServiceId())) {
            return recordReason(ServiceReason.IsValid(path, "svcMatch"));
        }

        //incrementStat(ServiceReason.ReasonCode.InflightChangeOfService);
        return recordReason(ServiceReason.InflightChangeOfService(service, path));
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
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> logger.info(format("%s: %s", code, statistics.get(code))));
    }

    public ServiceReason interestedInHour(Path path, int hour, LocalTime journeyClockTime) {
        totalChecked.getAndIncrement();

        int queryTimeHour = queryTime.getHour();
        if (hour == queryTimeHour) {
            // quick win
            return recordReason(ServiceReason.IsValid(path, "Hour"));
        }

        TramTime latestTimeInHour = TramTime.of(hour, 59);

        TramTime earliestTimeInHour = TramTime.of(LocalTime.of(hour,0).minusMinutes(maxWaitMinutes));
        TramTime earliestTime = TramTime.of(journeyClockTime);

        if (earliestTime.between(earliestTimeInHour,latestTimeInHour)) {
            return recordReason(ServiceReason.IsValid(path, "Hour"));
        }

        return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, earliestTimeInHour.toString(), path));
    }

    public void reportReasons() {
        reportStats();
        if (logger.isDebugEnabled()) {
            reasons.forEach(reason -> logger.debug("ServiceReason: " + reason ));
            createGraphFile();
        }
    }

    private void createGraphFile() {
        String fileName = format("%s_at_%s.dot", queryTime.toString(), LocalTime.now().toString());
        fileName = fileName.replaceAll(":","");

        if (reasons.isEmpty()) {
            logger.warn(format("Not creating dot file %s, reasons empty", fileName));
            return;
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            Set<String> paths = new HashSet<>();
            reasons.forEach(reason -> reason.recordPath(paths));
            paths.forEach(builder::append);
            builder.append("}");

            FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();
            logger.info(format("Created file %s", fileName));
        }
        catch (IOException e) {
            logger.warn("Unable to create diagnostic graph file", e);
        }
    }

    private ServiceReason recordReason(ServiceReason serviceReason) {
        if (logger.isDebugEnabled()) {
            reasons.add(serviceReason);
        }
        incrementStat(serviceReason.getReasonCode());
        return serviceReason;
    }

    public boolean toEndStation(Relationship depart) {
        return depart.getProperty(GraphStaticKeys.STATION_ID).toString().equals(endStationId);
    }

    public boolean matchesRoute(String routeId) {
        return preferRoutes.contains(routeId);
    }

    public ServiceReason canReachDestination(Node endNode, Path path) {
        String stationId = endNode.getProperty(ID).toString();
        boolean flag = reachabilityRepository.reachable(stationId, endStationId);
        if (flag) {
            return recordReason(ServiceReason.IsValid(path, "reachable station"));
        }
        return recordReason(ServiceReason.StationNotReachable(path, "unreachable from current stations"));

    }

    public boolean journeyTookTooLong(TramTime visitingTime) {
        return TramTime.diffenceAsMinutes( TramTime.of(queryTime), visitingTime)>maxJourneyDuration;
    }
}
