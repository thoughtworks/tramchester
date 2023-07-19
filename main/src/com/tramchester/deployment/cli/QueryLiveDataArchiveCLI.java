package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.livedata.cloud.FindUniqueDueTramStatus;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

public class QueryLiveDataArchiveCLI extends BaseCLI {
    private final Path outputFilename;

    public QueryLiveDataArchiveCLI(Path outputFilename) {
        this.outputFilename = outputFilename;
    }

    public static void main(String[] args) {

        /// NOTE: PLACE env variable overrides config file, see config files

        Logger logger = LoggerFactory.getLogger(QueryLiveDataArchiveCLI.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 2 arguments: <config filename> <output filename>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        logger.info("Config from " + configFile);

        Path outputFile = Paths.get(args[1]).toAbsolutePath();
        logger.info("Output filename " + outputFile);

        QueryLiveDataArchiveCLI fetchDataCLI = new QueryLiveDataArchiveCLI(outputFile);

        try {
            fetchDataCLI.run(configFile, logger, "FetchDataCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed", e);
            System.exit(-1);
        }
        logger.info("Success");

    }

    // TODO Doesn't work well, too much data and no way to slice and pause/resume on the processing
    // Likely need to have date and time range as parameters and then a way to track how far we've got

    @Override
    public void run(Logger logger, GuiceContainerDependencies dependencies) {
        FindUniqueDueTramStatus finder = dependencies.get(FindUniqueDueTramStatus.class);

        try {
            FileOutputStream outputStream = new FileOutputStream(outputFilename.toFile());
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);

            Stream<String> allStatus = finder.getAllDueTramStatus();

            // limited the amount of output for testing
//            Duration duration = Duration.of(1, ChronoUnit.HOURS);
//            LocalDateTime start = LocalDateTime.now().minus(duration);
//            Stream<String> allStatus = finder.getUniqueDueTramStatus(start, duration).stream();

            Stream<String> withLineSep = allStatus.
                    filter(text -> !UpcomingDeparture.KNOWN_STATUS.contains(text)).
                    map(text -> text + System.lineSeparator());

            logger.info("Writing tram status to " + outputFilename);
            withLineSep.forEach(line -> {
                try {
                    writer.write(line);
                } catch (IOException e) {
                    logger.error("Unable to write line");
                }
            });

            writer.flush();
            writer.close();
            logger.info("Done");

        } catch (IOException e) {
            logger.error("Exception " + e);
        }

    }
}
