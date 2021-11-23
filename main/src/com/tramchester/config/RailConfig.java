package com.tramchester.config;

import java.nio.file.Path;

public interface RailConfig {
    Path getDataPath();
    Path getStations();
    Path getTimetable();
}
