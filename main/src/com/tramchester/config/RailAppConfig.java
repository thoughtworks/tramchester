package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import io.dropwizard.Configuration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public class RailAppConfig extends Configuration implements RailConfig {

    private final Path dataPath;
    private final Path stations;
    private final Path timetable;

    public RailAppConfig(@JsonProperty(value ="dataPath", required = true) Path dataPath,
                         @JsonProperty(value ="stations", required = true)Path stations,
                         @JsonProperty(value ="timetable", required = true)Path timetable) {
        this.dataPath = dataPath;
        this.stations = stations;
        this.timetable = timetable;
    }

    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public boolean getOnlyMarkedInterchanges() {
        return true;
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.rail;
    }

    public Path getStations() {
        return stations;
    }

    public Path getTimetable() {
        return timetable;
    }

    @Override
    public Set<TransportMode> getModes() {
        return Collections.singleton(TransportMode.Train);
    }

}
