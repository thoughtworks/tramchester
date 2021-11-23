package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.SkippedRecord;
import com.tramchester.dataimport.rail.records.UnknownRecord;
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
    private final Map<String, RailRecordType> recordTypes;

    public RailTimetableDataFromFile(Path filePath, RailDataRecordFactory factory) {
        this.filePath = filePath.toAbsolutePath();
        this.factory = factory;
        recordTypes = createRecordTypes();
    }

    private Map<String, RailRecordType> createRecordTypes() {
        HashMap<String, RailRecordType> results = new HashMap<>();
        for (RailRecordType value : RailRecordType.values()) {
            results.put(value.code(), value);
        }
        return results;
    }

    public Stream<RailTimetableRecord> load() {
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

    public Stream<RailTimetableRecord> load(Reader in) {
        logger.info("Loading lines");
        BufferedReader bufferedReader = new BufferedReader(in);
        return bufferedReader.lines().map(this::processLine);
    }

    private RailTimetableRecord processLine(String line) {
        RailRecordType recordType = getRecordTypeFor(line);
        //logger.info("Processing " + recordType);
        return switch (recordType) {
            case TiplocInsert -> factory.createTIPLOC(line);
            case BasicSchedule -> factory.createBasicSchedule(line);
            case OriginLocation -> factory.createOrigin(line);
            case IntermediateLocation -> factory.createIntermediate(line);
            case TerminatingLocation -> factory.createTerminating(line);
            case BasicScheduleExtra -> factory.createBasicScheduleExtraDetails(line);
            case Header -> logHeader(line);
            case Association, ChangesEnRoute, Trailer
                    -> skipRecord(recordType, line);
            default -> throw new RuntimeException("Missing record type for " + line);
        };
    }

    private RailTimetableRecord skipRecord(RailRecordType recordType, String line) {
        // Record that for now we choose to ignore
        return new SkippedRecord(recordType, line);
    }

    private RailTimetableRecord logHeader(String line) {
        logger.info("Header: '" + line + "'");
        return new UnknownRecord(line);
    }

    private RailRecordType getRecordTypeFor(String line) {
        String rawType = line.substring(0, 2);
        if (recordTypes.containsKey(rawType)) {
            return recordTypes.get(rawType);
        }
        return RailRecordType.Unknown;
    }

}
