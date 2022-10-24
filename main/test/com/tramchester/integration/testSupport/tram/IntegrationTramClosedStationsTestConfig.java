package com.tramchester.integration.testSupport.tram;

import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class IntegrationTramClosedStationsTestConfig extends IntegrationTramTestConfig {


    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final boolean planningEnabled;

    public IntegrationTramClosedStationsTestConfig(List<StationClosures> closures, boolean planningEnabled) {
        super(createDBName(closures),  closures);
        this.planningEnabled = planningEnabled;
    }

    private static String createDBName(List<StationClosures> closures) {
        StringBuilder builder = new StringBuilder();
        builder.append("closed_");
        closures.forEach(closed -> {
            TramDate begin = closed.getBegin();
            TramDate end = closed.getEnd();

            builder.append(begin.format(formatter)).append("_").append(end.format(formatter)).append("_");

            closed.getStations().forEach(stationId -> builder.append(stationId.getGraphId()).append("_"));
        });
        builder.append("_tram.db");
        return builder.toString();
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

}
