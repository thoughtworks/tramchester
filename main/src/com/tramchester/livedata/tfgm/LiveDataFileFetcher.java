package com.tramchester.livedata.tfgm;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class LiveDataFileFetcher implements LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataFileFetcher.class);
    private final Path path;

    public LiveDataFileFetcher(Path path) {
        this.path = path;
    }

    @Override
    public String fetch() {
        try {
            return FileUtils.readFileToString(path.toFile(),  StandardCharsets.US_ASCII);
        } catch (IOException excep) {
            logger.error("Unable to read from file "+path.toAbsolutePath(), excep);
        }
        return "";
    }
}
