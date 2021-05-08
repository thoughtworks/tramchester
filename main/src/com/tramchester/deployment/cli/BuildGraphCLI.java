package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import io.dropwizard.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuildGraphCLI extends BaseCLI {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(BuildGraphCLI.class);

        if (args.length != 1) {
            throw new RuntimeException("Expected 1 arguments: <config file>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        logger.info("Config from " + configFile);

        BuildGraphCLI buildGraphCLI = new BuildGraphCLI();
        try {
            buildGraphCLI.run(configFile, logger, "BuildGraphCLI");
        } catch (ConfigurationException | IOException e) {
            logger.error("Failed",e);
            System.exit(-1);
        }
    }

    public void run(Logger logger, GuiceContainerDependencies dependencies) {
        dependencies.get(StagedTransportGraphBuilder.Ready.class);
    }

}
