package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasDataPath;
import com.tramchester.config.TramchesterConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;

@LazySingleton
public class FetchFileModTime {

    public LocalDateTime getFor(Path filePath) {
        long localModMillis = filePath.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZoneId);
    }

    public LocalDateTime getFor(HasDataPath config) {
        Path dataPath = config.getDataPath();
        return getFor(dataPath);
    }

    public boolean exists(Path filePath) {
        return Files.exists(filePath);
    }
}
