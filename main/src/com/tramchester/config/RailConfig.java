package com.tramchester.config;

import com.tramchester.domain.reference.TransportMode;

import java.nio.file.Path;
import java.util.Set;

public interface RailConfig extends HasDataPath, TransportDataSourceConfig {
    Path getStations();
    Path getTimetable();
    Set<TransportMode> getModes();
    String getVersion();
}
