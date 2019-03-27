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

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

public class ServiceHeuristics implements PersistsBoardingTime {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private final TramchesterConfig config;
    private final Set<String> runningServices;
    private final Set<String> preferRoutes;
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
                             LocalTime queryTime, Set<String> runningServices, Set<String> preferRoutes) {
        this.nodeOperations = nodeOperations;
        this.config = config;

        this.costEvaluator = costEvaluator;
        this.maxWaitMinutes = config.getMaxWait();
        this.queryTime = queryTime;
        this.runningServices = runningServices;
        this.preferRoutes = preferRoutes;

        // for none edge per trip path
        boardingTime = Optional.empty();

        // diagnostics, needs debug
        reasons = new LinkedList<>();
    }
    
    // edge per trip
    // TODO change to TramTime
    public ServiceReason checkServiceDate(Node node, Path path) {
        totalChecked.incrementAndGet();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.contains(nodeServiceId)) {
            return recordReason(ServiceReason.IsValid(path,"dateDay"));
        }

        dateWrong.incrementAndGet();
        return recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));

    }

    public ServiceReason checkServiceTime(Path path, Node node, LocalTime currentElapsed) {
        totalChecked.incrementAndGet();

        // prepared to wait up to max wait for start of a service...
        LocalTime serviceStart = nodeOperations.getServiceEarliest(node).asLocalTime().minusMinutes(maxWaitMinutes);
        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = nodeOperations.getServiceLatest(node);
        String nodeServiceId = nodeOperations.getServiceId(node);

        TramTime currentClock = TramTime.of(currentElapsed);
        if (!currentClock.between(TramTime.of(serviceStart), serviceEnd)) {
            timeWrong.getAndIncrement();
            return recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, "ServiceNotRunning:"+nodeServiceId,
                    path));
        }

        return recordReason(ServiceReason.IsValid(path,"svcTimes"));

    }

    // edge per trip
    public ServiceReason checkTime(Path path, Node node, LocalTime currentElapsed) {
        totalChecked.getAndIncrement();

        LocalTime nodeTime = nodeOperations.getTime(node);
        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return recordReason(ServiceReason.IsValid(path, "timeNode"));
        }
        timeWrong.incrementAndGet();
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
            // if node based time check is working should not need to actually check edges by this point
//            return recordReason(ServiceReason.IsValid(path));
        }

        totalChecked.incrementAndGet();
        // already checked via service node for edge per trip
        String serviceId = goesToRelationship.getServiceId();
        if (!runningServices.contains(serviceId)) {
            dateWrong.incrementAndGet();
            return recordReason(ServiceReason.DoesNotRunOnQueryDate(serviceId, path));
        }

        if (!sameService(path, incoming, goesToRelationship).isValid()) {
            return recordReason(ServiceReason.InflightChangeOfService(serviceId, path));
        }

        ElapsedTime elapsedTimeProvider = new PathBasedTimeProvider(costEvaluator, path, this, queryTime);
        // all times for the service per edge
        if (!operatesOnTime(goesToRelationship.getTimesServiceRuns(), elapsedTimeProvider)) {
            timeWrong.incrementAndGet();
            return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime,
                    elapsedTimeProvider.getElapsedTime().toString(), path));
        }

        return recordReason(ServiceReason.IsValid(path,"ok"));
    }

    // caller records
    public ServiceReason sameService(Path path, TransportRelationship transportRelationship, GoesToRelationship outgoing) {
        if (!transportRelationship.isGoesTo()) {
            return recordReason(ServiceReason.IsValid(path,"notGoesTo")); // not a connecting/goes to relationship, no svc id
        }

        GoesToRelationship incoming = (GoesToRelationship) transportRelationship;
        String service = incoming.getServiceId();

        if (service.equals(outgoing.getServiceId())) {
            return recordReason(ServiceReason.IsValid(path, "svcMatch"));
        }

        inflightChange.incrementAndGet();
        return ServiceReason.InflightChangeOfService(service, path);
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

    public ServiceReason interestedInHour(Path path, int hour, LocalTime journeyClockTime) {
        totalChecked.getAndIncrement();

        int queryTimeHour = queryTime.getHour();
        if (hour== queryTimeHour) {
            // quick win
            return recordReason(ServiceReason.IsValid(path, "Hour"));
        }

        TramTime latestTimeInHour = TramTime.of(hour, 59);

        TramTime earliestTimeInHour = TramTime.of(LocalTime.of(hour,0).minusMinutes(maxWaitMinutes));
        TramTime earliestTime = TramTime.of(journeyClockTime);

        if (earliestTime.between(earliestTimeInHour,latestTimeInHour)) {
            return recordReason(ServiceReason.IsValid(path, "Hour"));
        }

        timeWrong.getAndIncrement();
        return recordReason(ServiceReason.DoesNotOperateOnTime(queryTime, earliestTimeInHour.toString(), path));
    }

    public ServiceReason sameTripAndService(Path path, Relationship inbound, Relationship outbound) {
        if (!inbound.isType(TransportRelationshipTypes.TRAM_GOES_TO)) {
            throw new RuntimeException("Only call this check for inbound TRAM_GOES_TO relationships");
        }

        String inboundSvcId = inbound.getProperty(SERVICE_ID).toString();
        String outboundSvcId = outbound.getProperty(SERVICE_ID).toString();
        if (!inboundSvcId.equals(outboundSvcId)) {
            inflightChange.getAndIncrement();
            return recordReason(ServiceReason.InflightChangeOfService(format("%s->%s", inboundSvcId, outboundSvcId), path));
        }

        // now check inbound trip is available on this outgoing service
        String outboundTrips = (outbound.getProperty(TRIPS).toString());
        String inboundTripId = inbound.getProperty(TRIP_ID).toString();
        if (outboundTrips.contains(inboundTripId))  {
            return recordReason(ServiceReason.IsValid(path,format("[%s}%s->%s", inboundTripId, inboundSvcId, outboundSvcId)));
        }
        inflightChange.getAndIncrement();
        return recordReason(ServiceReason.InflightChangeOfService(inboundTripId, path));
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

        if (reasons.isEmpty()) {
            logger.warn(format("Not creating dot file %s, reasons empty", fileName));
            return;
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            Set<String> paths = new HashSet<>();
            reasons.forEach(reason -> reason.recordPath(paths));
            paths.forEach(path -> builder.append(path));
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
        return serviceReason;
    }

    public void clearReasons() {
        reasons.clear();
    }

    public ServiceReason checkReboardAndSvcChanges(Path path, Relationship inbound, boolean inboundWasBoarding, Relationship outbound) {

        if (outbound.isType(TO_SERVICE)) {
            if (inboundWasBoarding) {
                return recordReason(ServiceReason.IsValid(path,"board"));
            }
            return sameTripAndService(path, inbound, outbound);
        }

        boolean departing = outbound.isType(DEPART) || outbound.isType(INTERCHANGE_DEPART);
        if (inboundWasBoarding && departing) {
            return recordReason(ServiceReason.Reboard("reboard", path));
        }

        return recordReason(ServiceReason.IsValid(path, "no reboard"));
    }
}
