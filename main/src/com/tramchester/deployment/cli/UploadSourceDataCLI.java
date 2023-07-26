package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.deployment.UploadRemoteSourceData;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UploadSourceDataCLI extends BaseCLI {

    private final String s3Preifx;

    public UploadSourceDataCLI(String s3Preifx) {
        super();

        this.s3Preifx = s3Preifx;
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(UploadSourceDataCLI.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 1 arguments: <config filename>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        String s3Preifx = args[1];

        logger.info("Config from " + configFile + " s3 prefix: " + s3Preifx);

        UploadSourceDataCLI uploadSourceDataCLI = new UploadSourceDataCLI(s3Preifx);

        try {
            uploadSourceDataCLI.run(configFile, logger, "UploadSourceDataCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed", e);
            System.exit(-1);
        }
        logger.info("Success");
    }

    @Override
    public void run(Logger logger, GuiceContainerDependencies dependencies, TramchesterConfig config) {
        UnzipFetchedData unzipFetchedData = dependencies.get(UnzipFetchedData.class);
        unzipFetchedData.getReady();

        UploadRemoteSourceData uploadRemoteData = dependencies.get(UploadRemoteSourceData.class);
        uploadRemoteData.upload(s3Preifx);
    }
}
