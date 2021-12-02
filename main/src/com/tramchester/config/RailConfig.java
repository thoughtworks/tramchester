package com.tramchester.config;

import java.nio.file.Path;

public interface RailConfig extends HasDataPath, TransportDataSourceConfig {
    Path getStations();
    Path getTimetable();
}
