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

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.*;
import static java.lang.String.format;

public class ServiceHeuristics implements PersistsBoardingTime, BasicServiceHeuristics {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private static final boolean debugEnabled = logger.isDebugEnabled();

    private final TramchesterConfig config;
    private final RunningServices runningServices;
    private final String endStationId;
    private final TramTime queryTime;
    private final List<ServiceReason> reasons;
    private final ReachabilityRepository reachabilityRepository;

    private final CostEvaluator<Double> costEvaluator;
    private final CachedNodeOperations nodeOperations;
    private final int maxJourneyDuration;

    private Optional<TramTime> boardingTime;
    private final int maxWaitMinutes;

    // stats
    private final Map<ServiceReason.ReasonCode,AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);

    public ServiceHeuristics(CostEvaluator<Double> costEvaluator, CachedNodeOperations nodeOperations,
                             ReachabilityRepository reachabilityRepository, TramchesterConfig config, TramTime queryTime,
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

        statistics = new EnumMap<>(ServiceReason.ReasonCode.class);
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }
    
    // edge per trip
    public ServiceReason checkServiceDate(Node node, Path path) {
        totalChecked.incrementAndGet();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.isRunning(nodeServiceId)) {
            return valid(path);
        }

        return notOnQueryDate(path, nodeServiceId);
    }

    private void incrementStat(ServiceReason.ReasonCode reasonCode) {
        statistics.get(reasonCode).incrementAndGet();
    }

    public ServiceReason checkServiceTime(Path path, Node node, TramTime currentClock) {
        totalChecked.incrementAndGet();

        String serviceId = nodeOperations.getServiceId(node);

        // prepared to wait up to max wait for start of a service...
        TramTime serviceStart = runningServices.getServiceEarliest(serviceId).minusMinutes(maxWaitMinutes);

        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = runningServices.getServiceLatest(serviceId);

        if (!currentClock.between(serviceStart, serviceEnd)) {
            return recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, path));
        }

        return valid(path);
    }

    // edge per trip
    public ServiceReason checkTime(Path path, Node node, TramTime currentElapsed) {
        totalChecked.getAndIncrement();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return valid(path);
        }
        return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, path));
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
            return notOnQueryDate(path, serviceId);
        }

        ServiceReason inflightChange = sameService(path, incoming, goesToRelationship);
        if (!inflightChange.isValid()) {
            return recordReason(inflightChange);
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!underMaxWait(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            return recordReason(ServiceReason.DoesNotOperateOnTime(elapsedTimeProvider.getElapsedTime(),
                    path));
        }

        return valid(path);
    }

    public ServiceReason sameService(Path path, TransportRelationship transportRelationship, GoesToRelationship outgoing) {
        if (!transportRelationship.isGoesTo()) {
            return valid(path); // not a connecting/goes to relationship, no svc id
        }

        GoesToRelationship incoming = (GoesToRelationship) transportRelationship;
        String service = incoming.getServiceId();

        if (service.equals(outgoing.getServiceId())) {
            return valid(path);
        }

        return recordReason(ServiceReason.InflightChangeOfService(service, path));
    }

    public boolean underMaxWait(TramTime[] times, ElapsedTime provider) throws TramchesterException {
        if (times.length==0) {
            logger.warn("No times provided");
        }
        totalChecked.getAndIncrement();
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

    public void reportStats() {
        logger.info("Total checked: " + totalChecked.get());
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> logger.info(format("%s: %s", code, statistics.get(code))));
    }

    public ServiceReason interestedInHour(Path path, int hour, TramTime journeyClockTime) {
        totalChecked.getAndIncrement();

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
            if (journeyClockTime.getMinuteOfHour() >= 60-maxWaitMinutes) {
                return valid(path);
            }
        }

        return recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, path));
    }

    public void reportReasons() {
        reportStats();
        if (debugEnabled) {
            createGraphFile();
        }
    }

    private void createGraphFile() {
        String fileName = format("%s%s_at_%s.dot", queryTime.getHourOfDay(), queryTime.getMinuteOfHour(), LocalTime.now().toString());
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

    private ServiceReason recordReason(final ServiceReason serviceReason) {
        if (debugEnabled) {
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
            return valid(path);
        }
        return recordReason(ServiceReason.StationNotReachable(path));
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final Path path) {
        if (totalCost>maxJourneyDuration) {
            return recordReason(ServiceReason.TookTooLong(queryTime.plusMinutes(totalCost), path));
        }

        return valid(path);
    }

    private ServiceReason valid(final Path path) {
        if (debugEnabled) {
            return recordReason(ServiceReason.IsValid(path));
        }
        return recordReason(ServiceReason.isValid);
    }

    private ServiceReason notOnQueryDate(Path path, String nodeServiceId) {
        if (debugEnabled) {
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));
        }
        return recordReason(ServiceReason.DoesNotRunOnQueryDate());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }
}
