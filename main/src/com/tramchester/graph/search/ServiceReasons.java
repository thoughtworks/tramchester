package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class ServiceReasons {

    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(ServiceReasons.class);
    }

    private final TramTime queryTime;
    private final ProvidesNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final List<ServiceReason> reasons;
    // stats
    private final Map<ServiceReason.ReasonCode, AtomicInteger> reasonCodeStats; // reason -> count
    private final Map<String, AtomicInteger> stateStats; // State -> num visits
    private final Map<Long, AtomicInteger> nodeVisits; // count of visits to nodes
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean diagnosticsEnabled;

    private boolean success;

    public ServiceReasons(JourneyRequest journeyRequest, TramTime queryTime, ProvidesNow providesLocalNow) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        reasons = new ArrayList<>();
        success = false;
        diagnosticsEnabled = journeyRequest.getDiagnosticsEnabled();

        reasonCodeStats = new EnumMap<>(ServiceReason.ReasonCode.class);
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));

        stateStats = new HashMap<>();
        nodeVisits = new HashMap<>();
    }

    private void reset() {
        reasons.clear();
        reasonCodeStats.clear();
        stateStats.clear();
        nodeVisits.clear();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));
    }

    public void reportReasons(Transaction transaction, RouteCalculatorSupport.PathRequest pathRequest, ReasonsToGraphViz reasonToGraphViz) {
        if (diagnosticsEnabled) {
            createGraphFile(transaction, reasonToGraphViz, pathRequest);
        }

        if (!success || diagnosticsEnabled) {
            reportStats(transaction, pathRequest);
        }

        reset();
    }

    private void reportStats(Transaction txn, RouteCalculatorSupport.PathRequest pathRequest) {
        if ((!success) && journeyRequest.getWarnIfNoResults()) {
            logger.warn("No result found for at " + pathRequest.getActualQueryTime() + " changes " + pathRequest.getNumChanges() +
                    " for " + journeyRequest );
        }
        logger.info("Service reasons for query time: " + queryTime);
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        logStats("reasoncodes", reasonCodeStats);
        logStats("states", stateStats);
        logVisits(5000, txn);
    }

    private void logVisits(int threshhold, Transaction txn) {
        nodeVisits.entrySet().stream().
                map(entry -> Pair.of(entry.getKey(), entry.getValue().get())).
                filter(entry -> entry.getValue()>threshhold).
                sorted(Comparator.comparing(Pair::getValue, Comparator.reverseOrder())).
                limit(20).
                map(entry -> Pair.of(txn.getNodeById(entry.getKey()), entry.getValue())).
                map(pair -> Pair.of(nodeDetails(pair.getKey()), pair.getValue())).
                forEach(entry -> logger.info("Visited " + entry.getKey() + " " + entry.getValue() + " times"));
    }

    private String nodeDetails(Node node) {
        StringBuilder labels = new StringBuilder();
        node.getLabels().forEach(label -> labels.append(" ").append(label));
        return labels + " " + node.getAllProperties().toString();
    }

    private void logStats(String prefix, Map<?, AtomicInteger> stats) {
        stats.entrySet().stream().
                filter(entry -> entry.getValue().get() > 0).
                sorted(Comparator.comparingInt(a -> a.getValue().get())).
                forEach(entry -> logger.info(format("%s => %s: %s", prefix, entry.getKey(), entry.getValue().get())));
    }

    public ServiceReason recordReason(final ServiceReason serviceReason) {
        if (diagnosticsEnabled) {
            reasons.add(serviceReason);
        }

        // todo push into diag only if too costly here
        recordEndNodeVisit(serviceReason.getHowIGotHere());

        incrementStat(serviceReason.getReasonCode());
        return serviceReason;
    }

    private void recordEndNodeVisit(HowIGotHere howIGotHere) {
        long endNode = howIGotHere.getEndNodeId();
        if (nodeVisits.containsKey(endNode)) {
            nodeVisits.get(endNode).incrementAndGet();
        } else {
            nodeVisits.put(endNode, new AtomicInteger(1));
        }
    }

    public void incrementTotalChecked() {
        totalChecked.incrementAndGet();
    }

    private void incrementStat(ServiceReason.ReasonCode reasonCode) {
        reasonCodeStats.get(reasonCode).incrementAndGet();
    }

    public void recordSuccess() {
        incrementStat(ServiceReason.ReasonCode.Arrived);
        success = true;
    }

    public void recordStat(final ImmutableJourneyState journeyState) {
        ServiceReason.ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementStat(reason);

        String name = journeyState.getTraversalState().getClass().getSimpleName();
        if (stateStats.containsKey(name)) {
            stateStats.get(name).incrementAndGet();
        } else {
            stateStats.put(name, new AtomicInteger(1));
        }
    }

    private ServiceReason.ReasonCode getReasonCode(TransportMode transportMode) {
        return switch (transportMode) {
            case Tram -> ServiceReason.ReasonCode.OnTram;
            case Bus, RailReplacementBus -> ServiceReason.ReasonCode.OnBus;
            case Train -> ServiceReason.ReasonCode.OnTrain;
            case Walk, Connect -> ServiceReason.ReasonCode.OnWalk;
            case Ferry, Ship -> ServiceReason.ReasonCode.OnShip;
            case Subway -> ServiceReason.ReasonCode.OnSubway;
            case NotSet -> ServiceReason.ReasonCode.NotOnVehicle;
            case Unknown -> throw new RuntimeException("Unknown transport mode");
        };
    }

    private void createGraphFile(Transaction txn, ReasonsToGraphViz reasonsToGraphViz, RouteCalculatorSupport.PathRequest pathRequest) {
        String fileName = createFilename(pathRequest);

        if (reasons.isEmpty()) {
            logger.warn(format("Not creating dot file %s, reasons empty", fileName));
            return;
        } else {
            logger.warn("Creating diagnostic dot file: " + fileName);
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            reasonsToGraphViz.appendTo(builder, reasons, txn);
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

    private String createFilename(RouteCalculatorSupport.PathRequest pathRequest) {
        String status = success ? "found" : "notfound";
        String dateString = providesLocalNow.getDateTime().toLocalDate().toString();
        String changes = "changes" + pathRequest.getNumChanges();
        String postfix = journeyRequest.getUid().toString();
        String fileName = format("%s_%s%s_at_%s_%s_%s.dot", status,
                queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                dateString, changes, postfix);
        fileName = fileName.replaceAll(":","");
        return fileName;
    }


}
