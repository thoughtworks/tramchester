package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Service;
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

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.*;
import static java.lang.String.format;

public class ServiceHeuristics implements PersistsBoardingTime, BasicServiceHeuristics {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final RunningServices runningServices;
    private final String endStationId;
    private final LocalTime queryTime;
    private final List<ServiceReason> reasons;
    private final ReachabilityRepository reachabilityRepository;

    private final CostEvaluator<Double> costEvaluator;
    private final CachedNodeOperations nodeOperations;
    private final int maxJourneyDuration;

    private Optional<LocalTime> boardingTime;
    private final int maxWaitMinutes;

    // stats
    private final Map<ServiceReason.ReasonCode,AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, CachedNodeOperations nodeOperations,
                             ReachabilityRepository reachabilityRepository, TramchesterConfig config, LocalTime queryTime,
                             RunningServices runningServices, String endStationId) {
        this.nodeOperations = nodeOperations;
        this.reachabilityRepository = reachabilityRepository;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.maxJourneyDuration = config.getMaxJourneyDuration();
        this.queryTime = queryTime;
        this.runningServices = runningServices;
        this.endStationId = endStationId;

        // for none edge per trip path
        boardingTime = Optional.empty();

        // diagnostics, needs debug
        reasons = new LinkedList<>();

        statistics = new HashMap<>();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }
    
    // edge per trip
    public ServiceReason checkServiceDate(Node node, Path path) {
        totalChecked.incrementAndGet();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.isRunning(nodeServiceId)) {
            return recordReason(ServiceReason.IsValid(path,"dateDay"));
        }

        return recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));
    }

    private void incrementStat(ServiceReason.ReasonCode reasonCode) {
        statistics.get(reasonCode).incrementAndGet();
    }

    public ServiceReason checkServiceTime(Path path, Node node, TramTime currentClock) {
        totalChecked.incrementAndGet();

        // prepared to wait up to max wait for start of a service...
        LocalTime serviceStart = nodeOperations.getServiceEarliest(node).asLocalTime().minusMinutes(maxWaitMinutes);
        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = nodeOperations.getServiceLatest(node);

        if (!currentClock.between(TramTime.of(serviceStart), serviceEnd)) {
            String nodeServiceId = nodeOperations.getServiceId(node);
            return recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, "ServiceNotRunning:"+nodeServiceId,
                    path));
        }

        return recordReason(ServiceReason.IsValid(path,"svcTimes"));
    }

    // edge per trip
    public ServiceReason checkTime(Path path, Node node, TramTime currentElapsed) {
        totalChecked.getAndIncrement();

        TramTime nodeTime = TramTime.of(nodeOperations.getTime(node));
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, "after", path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return recordReason(ServiceReason.IsValid(path, "timeNode"));
        }
        return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, "outside", path));
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

        totalChecked.incrementAndGet();
        // already checked via service node for edge per trip
        String serviceId = goesToRelationship.getServiceId();
        if (!runningServices.isRunning(serviceId)) {
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(serviceId, path));
        }

        ServiceReason inflightChange = sameService(path, incoming, goesToRelationship);
        if (!inflightChange.isValid()) {
            return recordReason(inflightChange);
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!underMaxWait(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            return recordReason(ServiceReason.DoesNotOperateOnTime(TramTime.of(elapsedTimeProvider.getElapsedTime()),
                    "max wait", path));
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

        return recordReason(ServiceReason.InflightChangeOfService(service, path));
    }

    public boolean underMaxWait(LocalTime[] times, ElapsedTime provider) throws TramchesterException {
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

    public ServiceReason interestedInHour(Path path, int hour, TramTime journeyClockTime) {
        totalChecked.getAndIncrement();

        int queryTimeHour = queryTime.getHour();
        if (hour == queryTimeHour) {
            // quick win
            return recordReason(ServiceReason.IsValid(path, "Hour Matches"));
        }

        TramTime latestTimeInHour = TramTime.of(hour, 59);

        TramTime earliestTimeInHour = TramTime.of(LocalTime.of(hour,0).minusMinutes(maxWaitMinutes));
        //TramTime earliestTime = TramTime.of(journeyClockTime);

        if (journeyClockTime.between(earliestTimeInHour,latestTimeInHour)) {
            return recordReason(ServiceReason.IsValid(path, "Hour Range Matches"));
        }

        return recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime,
                earliestTimeInHour.toString()+"->"+latestTimeInHour.toString(), path));
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
            reasons.stream().filter(reason->!reason.isValid()).forEach(reason -> reason.recordPath(paths));
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

    public ServiceReason canReachDestination(Node endNode, Path path) {
        String stationId = endNode.getProperty(ID).toString();
        boolean flag = reachabilityRepository.reachable(stationId, endStationId);
        if (flag) {
            return recordReason(ServiceReason.IsValid(path, "reachable station"));
        }
        return recordReason(ServiceReason.StationNotReachable(path));

    }

    public ServiceReason journeyDurationUnderLimit(TramTime visitingTime, Path path) {
        if (TramTime.diffenceAsMinutes( TramTime.of(queryTime), visitingTime)>maxJourneyDuration) {
            return recordReason(ServiceReason.TookTooLong(visitingTime, path));
        }
        return recordReason(ServiceReason.IsValid(path,"not too long"));
    }

    public ServiceReason overMaxWait(TramTime journeyClock, TramTime previousVisit, Path path) {
        int diffenceAsMinutes = TramTime.diffenceAsMinutes(previousVisit, journeyClock);

        if (journeyClock.isAfter(previousVisit) && diffenceAsMinutes > maxWaitMinutes) {
            return recordReason(ServiceReason.DoesNotOperateOnTime(journeyClock, "max wait", path));
        }

        return recordReason(ServiceReason.IsValid(path, "Early visit or within 30 mins"));
    }
}
