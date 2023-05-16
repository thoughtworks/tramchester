package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.SkippedRecord;
import com.tramchester.dataimport.rail.records.UnknownRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;


@LazySingleton
public class LoadRailTimetableRecords implements ProvidesRailTimetableRecords {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTimetableRecords.class);

    private final Path filePath;
    private final RailDataRecordFactory factory;
    private final boolean enabled;

    @Inject
    public LoadRailTimetableRecords(TramchesterConfig config, RailDataRecordFactory factory, UnzipFetchedData.Ready ready) {
        RailConfig railConfig = config.getRailConfig();
        enabled = (railConfig != null);
        this.factory = factory;
        if (enabled) {
            final Path dataPath = railConfig.getDataPath();
            filePath = dataPath.resolve(railConfig.getTimetable());
        } else {
            filePath = null;
        }
    }

    @Override
    public Stream<RailTimetableRecord> load() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }

        logger.info("Load from " + filePath.toAbsolutePath());
        try {
            Reader reader = new FileReader(filePath.toString(), StandardCharsets.US_ASCII);
            return load(reader);
        } catch (IOException e) {
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

    private RailRecordType getRecordTypeFor(CharSequence line) {
        return RailRecordType.parse(line.subSequence(0,2));
    }

    @Override
    public String toString() {
        return "LoadRailTimetableRecords{" +
                "filePath=" + filePath +
                ", enabled=" + enabled +
                '}';
    }
}
