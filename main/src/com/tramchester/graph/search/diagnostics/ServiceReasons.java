package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
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
    public static final int NUMBER_MOST_VISITED_NODES_TO_LOG = 20;
    public static final int THRESHHOLD_FOR_NUMBER_VISITS_DIAGS = 5000;

    static {
        logger = LoggerFactory.getLogger(ServiceReasons.class);
    }

    private final TramTime queryTime;
    private final ProvidesNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final List<HeuristicsReason> reasons;
    // stats
    private final Map<ReasonCode, AtomicInteger> reasonCodeStats; // reason -> count
    private final Map<TraversalStateType, AtomicInteger> stateStats; // State -> num visits
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

        reasonCodeStats = new EnumMap<>(ReasonCode.class);
        Arrays.asList(ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));

        stateStats = new EnumMap<>(TraversalStateType.class);
        nodeVisits = new HashMap<>();
    }

    private void reset() {
        reasons.clear();
        reasonCodeStats.clear();
        stateStats.clear();
        nodeVisits.clear();
        Arrays.asList(ReasonCode.values()).forEach(code -> reasonCodeStats.put(code, new AtomicInteger(0)));
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
        logVisits(txn);
    }

    private void logVisits(Transaction txn) {
        nodeVisits.entrySet().stream().
                map(entry -> Pair.of(entry.getKey(), entry.getValue().get())).
                filter(entry -> entry.getValue()>THRESHHOLD_FOR_NUMBER_VISITS_DIAGS).
                sorted(Comparator.comparing(Pair::getValue, Comparator.reverseOrder())).
                limit(NUMBER_MOST_VISITED_NODES_TO_LOG).
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

    public HeuristicsReason recordReason(final HeuristicsReason serviceReason) {
        if (diagnosticsEnabled) {
            reasons.add(serviceReason);
            recordEndNodeVisit(serviceReason.getHowIGotHere());
        } else {
            if (!serviceReason.isValid()) {
                recordEndNodeVisit(serviceReason.getHowIGotHere());
            }
        }

        incrementStat(serviceReason.getReasonCode());
        return serviceReason;
    }

    private void recordEndNodeVisit(final HowIGotHere howIGotHere) {
        final long endNode = howIGotHere.getEndNodeId();
        if (nodeVisits.containsKey(endNode)) {
            nodeVisits.get(endNode).incrementAndGet();
        } else {
            nodeVisits.put(endNode, new AtomicInteger(1));
        }
    }

    public void incrementTotalChecked() {
        totalChecked.incrementAndGet();
    }

    private void incrementStat(ReasonCode reasonCode) {
        reasonCodeStats.get(reasonCode).incrementAndGet();
    }

    public void recordSuccess() {
        incrementStat(ReasonCode.Arrived);
        success = true;
    }

    public void recordStat(final ImmutableJourneyState journeyState) {
        final ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementStat(reason);

        final TraversalStateType stateType = journeyState.getTraversalState().getStateType();
        if (stateStats.containsKey(stateType)) {
            stateStats.get(stateType).incrementAndGet();
        } else {
            stateStats.put(stateType, new AtomicInteger(1));
        }
    }

    private ReasonCode getReasonCode(TransportMode transportMode) {
        return switch (transportMode) {
            case Tram -> ReasonCode.OnTram;
            case Bus, RailReplacementBus -> ReasonCode.OnBus;
            case Train -> ReasonCode.OnTrain;
            case Walk, Connect -> ReasonCode.OnWalk;
            case Ferry, Ship -> ReasonCode.OnShip;
            case Subway -> ReasonCode.OnSubway;
            case NotSet -> ReasonCode.NotOnVehicle;
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
