package com.tramchester.deployment.cli;

import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import io.dropwizard.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public class BuildGraphCLI extends BaseCLI {

    private final Path destinationRoot;

    public BuildGraphCLI(Path destinationRoot) {
        super();
        this.destinationRoot = destinationRoot;
    }

    public static void main(String[] args)  {
        Logger logger = LoggerFactory.getLogger(BuildGraphCLI.class);

        if (args.length != 2) {
            throw new RuntimeException("Expected 2 arguments: <config file> <destination directory>");
        }
        Path configFile = Paths.get(args[0]).toAbsolutePath();
        logger.info("Config from " + configFile);

        Path destination = Paths.get(args[1]).toAbsolutePath();

        try {
            if (!Files.exists(destination)) {
                Files.createDirectories(destination);
            }
            if (!Files.isDirectory(destination)) {
                throw new RuntimeException(format("Destination '%s' must be a directory", destination));
            }

            BuildGraphCLI buildGraphCLI = new BuildGraphCLI(destination);

            buildGraphCLI.run(configFile, logger, "BuildGraphCLI");

        } catch (ConfigurationException | IOException e) {
            logger.error("Failed",e);
            System.exit(-1);
        }
    }

    public void run(Logger logger, GuiceContainerDependencies dependencies) {
        TramchesterConfig config = dependencies.get(TramchesterConfig.class);
        Path original = config.getGraphDBConfig().getDbPath();
        dependencies.get(StagedTransportGraphBuilder.Ready.class);
        dependencies.close();

        Path destination = destinationRoot.resolve(original.getFileName());

        logger.info(format("Copy from %s to %s", original.toAbsolutePath(), destination.toAbsolutePath()));

        try {
            FileUtils.copyDirectory(original.toFile(), destination.toFile());
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to copy from %s to %s",
                    original.toAbsolutePath(), destination.toAbsolutePath()), e);
        }
    }

}
