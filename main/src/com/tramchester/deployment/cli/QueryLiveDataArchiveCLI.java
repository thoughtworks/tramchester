package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static java.lang.String.format;

public class QueryLiveDataArchiveCLI extends BaseCLI {
    private final Path outputFilename;
    private final TramDate date;
    private final int days;
    private final Duration sampleWindow;

    public QueryLiveDataArchiveCLI(Path outputFilename, TramDate date, int days, Duration sampleWindow) {
        this.outputFilename = outputFilename;
        this.date = date;
        this.days = days;
        this.sampleWindow = sampleWindow;
    }

    public static void main(String[] args) {

        /// NOTE: PLACE env variable overrides config file, see config files

        Logger logger = LoggerFactory.getLogger(QueryLiveDataArchiveCLI.class);

        if (args.length != 5) {
            String message = "Expected 4 arguments: <date> <days> <minutes> <config filename> <output filename>";
            logger.error(message);
            throw new RuntimeException(message);
        }

        TramDate date = TramDate.parse(args[0]);
        logger.info("Date " + date);

        int days = Integer.parseInt(args[1]);
        logger.info("Days " + days);

        int mins = Integer.parseInt(args[2]);
        logger.info("Sample Window Minutes " + mins);

        Path configFile = Paths.get(args[3]).toAbsolutePath();
        logger.info("Config from " + configFile);

        Path outputFile = Paths.get(args[4]).toAbsolutePath();
        logger.info(format("Output filename %s date %s days %s", outputFile, date, days));

        QueryLiveDataArchiveCLI fetchDataCLI = new QueryLiveDataArchiveCLI(outputFile, date, days, Duration.ofMinutes(mins));

        try {
            fetchDataCLI.run(configFile, logger, "FetchDataCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed", e);
            System.exit(-1);
        }
        logger.info("Success");

    }


    @Override
    public void run(Logger logger, GuiceContainerDependencies dependencies, TramchesterConfig config) {
        FindUniqueDueTramStatus finder = dependencies.get(FindUniqueDueTramStatus.class);

        boolean liveDataEnabled = config.liveTrainDataEnabled();
        if (!liveDataEnabled) {
            logger.warn("Live data is not enable for this config");
        }
        TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
        logger.info("s3bucket is " + liveDataConfig.getS3Bucket() + " s3prefix is " + liveDataConfig.getS3Prefix());

        try {
            FileOutputStream outputStream = new FileOutputStream(outputFilename.toFile());
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);

            // limited the amount of output for testing
            Duration duration = Duration.of(days, ChronoUnit.DAYS);
            LocalTime time = LocalTime.of(0,1); // one minute past midnight
            LocalDateTime start = LocalDateTime.of(date.toLocalDate(), time);
            Stream<String> allStatus = finder.getUniqueDueTramStatus(start, duration, sampleWindow).stream();

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
