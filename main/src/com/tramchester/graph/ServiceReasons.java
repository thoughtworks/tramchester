package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class ServiceReasons {

    private static final Logger logger = LoggerFactory.getLogger(ServiceReasons.class);
    private static final boolean debugEnabled = logger.isDebugEnabled();

    private final List<ServiceReason> reasons;
    // stats
    private final Map<ServiceReason.ReasonCode, AtomicInteger> statistics;
    private final AtomicInteger totalChecked = new AtomicInteger(0);
    private boolean success;

    public ServiceReasons() {
        reasons = new ArrayList<>();
        statistics = new EnumMap<>(ServiceReason.ReasonCode.class);
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
        success = false;
    }

    private void reset() {
        reasons.clear();
        statistics.clear();
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(code -> statistics.put(code, new AtomicInteger(0)));
    }

    public void reportReasons(TramTime queryTime) {
        if (success && !debugEnabled) {
            reset();
            return;
        }

        reportStats();
        if (debugEnabled) {
            createGraphFile(queryTime);
        }
        reset();
    }

    private void reportStats() {
        logger.info("Total checked: " + totalChecked.get());
        Arrays.asList(ServiceReason.ReasonCode.values()).forEach(
                code -> logger.info(format("%s: %s", code, statistics.get(code))));
    }

    public ServiceReason recordReason(final ServiceReason serviceReason) {
        if (debugEnabled) {
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

    private void createGraphFile(TramTime queryTime) {
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
//            reasons.stream().filter(reason->!reason.isValid()).forEach(reason -> reason.recordPath(paths));
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

    public void recordSuccess() {
        success = true;
    }

    public void record(final ImmutableJourneyState journeyState) {
        if (journeyState.onTram()) {
            incrementStat(ServiceReason.ReasonCode.OnTram);
        } else {
            incrementStat(ServiceReason.ReasonCode.OnBus);
        }
    }


}
