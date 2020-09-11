package com.tramchester.graph.search;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
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
    private final ProvidesLocalNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final int numChanges;
    private final List<ServiceReason> reasons;
    // stats
    private final Map<ServiceReason.ReasonCode, AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean diagnosticsEnabled;

    private boolean success;

    public ServiceReasons(JourneyRequest journeyRequest, TramTime queryTime, ProvidesLocalNow providesLocalNow, int numChanges) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        this.numChanges = numChanges;
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

    public void reportReasons(Transaction transaction, TransportData transportData) {
        if (diagnosticsEnabled) {
            createGraphFile(transaction, transportData);
        }

        if (!success || diagnosticsEnabled) {
            reportStats();
        }

        reset();
    }

    private void reportStats() {
        if ((!success) && journeyRequest.getWarnIfNoResults()) {
            logger.warn("No result found for " + journeyRequest.toString() + " at " + queryTime + " max changes " + numChanges);
        }
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> logger.info(format("%s: %s", code, statistics.get(code))));

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
        success = true;
    }

    public void recordStat(final ImmutableJourneyState journeyState) {
        ServiceReason.ReasonCode reason = getReasonCode(journeyState.getTransportMode());
        incrementStat(reason);
    }

    private ServiceReason.ReasonCode getReasonCode(TransportMode transportMode) {
        switch (transportMode) {
            case Tram: return ServiceReason.ReasonCode.OnTram;
            case Bus: return ServiceReason.ReasonCode.OnBus;
            case Train: return ServiceReason.ReasonCode.OnTrain;
            default: return ServiceReason.ReasonCode.NotOnVehicle;
        }
    }

    private void createGraphFile(Transaction transaction, TransportData transportData) {
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

            ReasonsToGraphViz reasonsToGraphViz = new ReasonsToGraphViz(transaction, transportData, builder);

            reasons.forEach(reasonsToGraphViz::add);

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

    @NotNull
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
