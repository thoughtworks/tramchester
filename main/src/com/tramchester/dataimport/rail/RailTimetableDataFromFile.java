package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.RailTimetableRecord;
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


public class RailTimetableDataFromFile {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableDataFromFile.class);

    private final Path filePath;
    private final RailDataRecordFactory factory;
    private final Map<String, RecordType> recordTypes;

    public enum RecordType {
        TI, // tiploc insert
        BS, // basic schedule
        BX, // basic schedule extra
        CR, // Changes En Route
        LI, // Intermediate Location
        LO, // Origin Location
        LT, // TerminatingLocation
        Unknown
    }

    public RailTimetableDataFromFile(Path filePath, RailDataRecordFactory factory) {
        this.filePath = filePath.toAbsolutePath();
        this.factory = factory;
        recordTypes = createRecordTypes();
    }

    private Map<String, RecordType> createRecordTypes() {
        HashMap<String, RecordType> results = new HashMap<>();
        for (RecordType value : RecordType.values()) {
            results.put(value.name(), value);
        }
        return results;
    }

    public Stream<RailTimetableRecord> load() {
        try {
            Reader reader = new FileReader(filePath.toString());
            return load(reader);
        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public Stream<RailTimetableRecord> load(Reader in) {
        logger.info("Loading lines");
        BufferedReader bufferedReader = new BufferedReader(in);
        return bufferedReader.lines().map(this::processLine);
    }

    private RailTimetableRecord processLine(String line) {
        RecordType recordType = getRecordTypeFor(line);
        logger.info("Processing " + recordType);
        return switch (recordType) {
            case TI -> factory.createTIPLOC(line);
            case BS -> factory.createBasicSchedule(line);
            case LO -> factory.createOrigin(line);
            case LI -> factory.createIntermediate(line);
            case LT -> factory.createTerminating(line);
            default -> throw new RuntimeException("Missing record type for " + line);
        };
    }

    private RecordType getRecordTypeFor(String line) {
        String rawType = line.substring(0, 2);
        if (recordTypes.containsKey(rawType)) {
            return recordTypes.get(rawType);
        }
        return RecordType.Unknown;
    }

}
