package com.tramchester.config;

import com.netflix.governator.guice.lazy.LazySingleton;

import java.nio.file.Path;

// TODO into actual configuration
@LazySingleton
public class RailConfig {
    public static final String CURRENT_PREFIX = "ttisf187";
    private final Path toDate = Path.of("data", "rail");

    private final Path railStationFile = toDate.resolve(Path.of(CURRENT_PREFIX +".msn"));
    private final Path railTimetableFile = toDate.resolve(Path.of(CURRENT_PREFIX+".mca"));

    public Path getRailStationFile() {
        return railStationFile;
    }

    public Path getRailTimetableFile() {
        return railTimetableFile;
    }
}
