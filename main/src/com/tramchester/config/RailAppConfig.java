package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import io.dropwizard.core.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// https://www.raildeliverygroup.com/our-services/rail-data/timetable-data.html
// https://data.atoc.org/how-to

public class RailAppConfig extends Configuration implements RailConfig {

    private final Path dataPath;
    private final Path stations;
    private final Path timetable;
    private final String version;
    private final EnumSet<TransportMode> modes;
    private final int initialWaitMinutes;

    public RailAppConfig(@JsonProperty(value ="dataPath", required = true) Path dataPath,
                         @JsonProperty(value ="version", required = true) String version,
                         @JsonProperty(value="modes", required = true) Set<TransportMode> modes,
                         @JsonProperty(value="initialWaitMinutes", required = true) int initialWaitMinutes) {
        this.dataPath = dataPath;
        this.modes = EnumSet.copyOf(modes);

        this.version = version;
        final String filename = "ttisf" + version;

        this.timetable = Path.of(String.format("%s.mca", filename));
        this.stations = Path.of(String.format("%s.msn", filename));

        this.initialWaitMinutes = initialWaitMinutes;
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

    @Override
    public Duration getMaxInitialWait() {
        return Duration.ofMinutes(initialWaitMinutes);
    }

    public Path getStations() {
        return stations;
    }

    public Path getTimetable() {
        return timetable;
    }

    @Override
    public Set<TransportMode> getModes() {
        return modes;
    }

    public String getVersion() {
        return version;
    }
}
