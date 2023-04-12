package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@LazySingleton
public class ProvidesRailStationRecords {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesRailStationRecords.class);

    private final Path filePath;
    private final Map<String, RecordType> recordTypes;
    private final boolean enabled;

    public enum RecordType {
        A, // phyiscal station
        L, // alias
        Z, // file trailer or Last Written by
        Unknown
    }

    @Inject
    public ProvidesRailStationRecords(TramchesterConfig config, UnzipFetchedData.Ready ready) {
        enabled = config.hasRailConfig();
        if (enabled) {
            RailConfig railConfig = config.getRailConfig();
            final Path dataPath = railConfig.getDataPath();
            this.filePath = dataPath.resolve(railConfig.getStations());
        } else {
            this.filePath = null;
        }
        recordTypes = createRecordTypes();
    }

    @PostConstruct
    private void start() {
        if (enabled) {
            logger.info("Started for " + filePath);
        } else {
            logger.info("Disabled");
        }
    }

    private Map<String, RecordType> createRecordTypes() {
        HashMap<String, RecordType> results = new HashMap<>();
        for (RecordType value : RecordType.values()) {
            results.put(value.name(), value);
        }
        return results;
    }

    public Stream<PhysicalStationRecord> load() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }

        logger.info("Load from " + filePath.toAbsolutePath());
        try {
            Reader reader = new FileReader(filePath.toString(),  StandardCharsets.US_ASCII);
            return load(reader);
        } catch (IOException e) {
            String msg = "Unable to load from file " + filePath.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Stream<PhysicalStationRecord> load(Reader in) {
        BufferedReader bufferedReader = new BufferedReader(in);
        return bufferedReader.lines().
                filter(line -> getRecordTypeFor(line).equals(RecordType.A)).
                map(this::createPhysicalStation);
    }

    private PhysicalStationRecord createPhysicalStation(String line) {
        return PhysicalStationRecord.parse(line);
    }

    private RecordType getRecordTypeFor(String line) {
        String rawType = line.substring(0, 1);
        if (recordTypes.containsKey(rawType)) {
            return recordTypes.get(rawType);
        }
        return RecordType.Unknown;
    }
}
