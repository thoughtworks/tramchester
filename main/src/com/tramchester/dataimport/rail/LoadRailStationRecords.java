package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class LoadRailStationRecords {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailStationRecords.class);

    private final Path filePath;
    private final Map<String, RecordType> recordTypes;

    public enum RecordType {
        A, // phyiscal station
        L, // alias
        Z, // file trailer or Last Written by
        Unknown
    }

    public LoadRailStationRecords(Path filePath) {
        this.filePath = filePath.toAbsolutePath();
        recordTypes = createRecordTypes();
    }

    private Map<String, RecordType> createRecordTypes() {
        HashMap<String, RecordType> results = new HashMap<>();
        for (RecordType value : RecordType.values()) {
            results.put(value.name(), value);
        }
        return results;
    }

    public Stream<PhysicalStationRecord> load() {
        logger.info("Load from " + filePath.toAbsolutePath());
        try {
            Reader reader = new FileReader(filePath.toString());
            return load(reader);
        } catch (FileNotFoundException e) {
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
