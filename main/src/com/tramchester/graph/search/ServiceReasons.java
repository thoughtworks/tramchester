package com.tramchester.graph.search;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class ServiceReasons {

    private static final Logger logger = LoggerFactory.getLogger(ServiceReasons.class);
    private static final boolean createDotFile = logger.isDebugEnabled();

    private final TramTime queryTime;
    private final ProvidesLocalNow providesLocalNow;
    private final JourneyRequest journeyRequest;
    private final List<ServiceReason> reasons;
    // stats
    private final Map<ServiceReason.ReasonCode, AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private final boolean statsOn;
    private boolean success;

    public ServiceReasons(JourneyRequest journeyRequest, TramTime queryTime, ProvidesLocalNow providesLocalNow) {
        this.queryTime = queryTime;
        this.providesLocalNow = providesLocalNow;
        this.journeyRequest = journeyRequest;
        reasons = new ArrayList<>();
        statistics = new EnumMap<>(ServiceReason.ReasonCode.class);
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
        success = false;
        statsOn = journeyRequest.getDiagnosticsEnabled();
    }

    private void reset() {
        reasons.clear();
        statistics.clear();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }

    public void reportReasons() {
        if (createDotFile) {
            createGraphFile();
        }

        if (!success || statsOn) {
            reportStats();
        }

        reset();
    }

    private void reportStats() {
        if (!success) {
            logger.warn("No result found for " + journeyRequest.toString() + " at " + queryTime);
        }
        logger.info("Total checked: " + totalChecked.get() + " for " + journeyRequest.toString());
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> logger.info(format("%s: %s", code, statistics.get(code))));

    }

    public ServiceReason recordReason(final ServiceReason serviceReason) {
        if (createDotFile) {
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

    public void record(final ImmutableJourneyState journeyState) {
        if (journeyState.onTram()) {
            incrementStat(ServiceReason.ReasonCode.OnTram);
        } if (journeyState.onBus()) {
            incrementStat(ServiceReason.ReasonCode.OnBus);
        }
        else {
            incrementStat(ServiceReason.ReasonCode.NotOnVehicle);
        }
    }

    private void createGraphFile() {
        String fileName = format("%s%s_at_%s.dot", queryTime.getHourOfDay(), queryTime.getMinuteOfHour(),
                providesLocalNow.getDateTime().toLocalDate().toString());
        fileName = fileName.replaceAll(":","");

        if (reasons.isEmpty()) {
            logger.warn(format("Not creating dot file %s, reasons empty", fileName));
            return;
        } else {
            logger.warn("Creating diagnostic dot file: " + fileName);
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");
            Set<String> paths = new HashSet<>();
            reasons.stream().forEach(reason -> reason.recordPath(paths));

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


}
