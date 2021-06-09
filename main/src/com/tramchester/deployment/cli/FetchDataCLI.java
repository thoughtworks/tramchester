package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.FetchDataFromUrl;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public class FetchDataCLI extends BaseCLI {

    // used during build to download latest tram data from tfgm site during deployment
    // which is subsequently uploaded into S3
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(FetchDataCLI.class);

        if (args.length != 1) {
            throw new RuntimeException("Expected 1 arguments: <config filename>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        logger.info("Config from " + configFile);

        FetchDataCLI fetchDataCLI = new FetchDataCLI();

        try {
            fetchDataCLI.run(configFile, logger, "FetchDataCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed", e);
            System.exit(-1);
        }
        logger.info("Success");
    }

    @Override
    public void run(Logger logger, GuiceContainerDependencies dependencies) {
        FetchDataFromUrl fetcher = dependencies.get(FetchDataFromUrl.class);
        fetcher.getReady();
    }
}
