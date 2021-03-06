package com.tramchester.graph.search;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
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
    private final Map<ServiceReason.ReasonCode, AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean diagnosticsEnabled;

    private boolean success;

    public ServiceReasons(JourneyRequest journeyRequest, TramTime queryTime, ProvidesNow providesLocalNow) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        reasons = new ArrayList<>();
        statistics = new EnumMap<>(ServiceReason.ReasonCode.class);
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
        success = false;
        diagnosticsEnabled = journeyRequest.getDiagnosticsEnabled();
    }

    private void reset() {
        reasons.clear();
        statistics.clear();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }

    public void reportReasons(Transaction transaction, RouteCalculatorSupport.PathRequest pathRequest, ReasonsToGraphViz reasonToGraphViz) {
        if (diagnosticsEnabled) {
            createGraphFile(transaction, reasonToGraphViz);
        }

        if (!success || diagnosticsEnabled) {
            reportStats(pathRequest);
        }

        reset();
    }

    private void reportStats(RouteCalculatorSupport.PathRequest pathRequest) {
        if ((!success) && journeyRequest.getWarnIfNoResults()) {
            logger.warn("No result found for at " + pathRequest.getActualQueryTime() + " changes " + pathRequest.getNumChanges() +
                    " for " + journeyRequest );
        }
        logger.info("Service reasons for query time: " + queryTime);
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> {
                    if (statistics.get(code).get() > 0) {
                        logger.info(format("%s: %s", code, statistics.get(code)));
                    }
                });
    }

    public ServiceReason recordReason(final ServiceReason serviceReason) {
        if (diagnosticsEnabled) {
            reasons.add(serviceReason);
        }
        incrementStat(serviceReason.getReasonCode());
        return serviceReason;
    }

    public void incrementTotalChecked() {
        totalChecked.incrementAndGet();
    }

    private void incrementStat(ServiceReason.ReasonCode reasonCode) {
        statistics.get(reasonCode).incrementAndGet();
    }

    public void recordSuccess() {
        incrementStat(ServiceReason.ReasonCode.Arrived);
        success = true;
    }

    public void recordStat(final ImmutableJourneyState journeyState) {
        ServiceReason.ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementStat(reason);
    }

    private ServiceReason.ReasonCode getReasonCode(TransportMode transportMode) {
        return switch (transportMode) {
            case Tram -> ServiceReason.ReasonCode.OnTram;
            case Bus -> ServiceReason.ReasonCode.OnBus;
            case Train -> ServiceReason.ReasonCode.OnTrain;
            default -> ServiceReason.ReasonCode.NotOnVehicle;
        };
    }

    private void createGraphFile(Transaction txn, ReasonsToGraphViz reasonsToGraphViz) {
        String fileName = createFilename();

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

    private String createFilename() {
        String postfix = journeyRequest.getUid().toString();
        String dateString = providesLocalNow.getDateTime().toLocalDate().toString();
        String status = success ? "found" : "notfound";
        String fileName = format("%s_%s%s_at_%s_%s.dot", status,
                queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                dateString, postfix);
        fileName = fileName.replaceAll(":","");
        return fileName;
    }


}
