package com.tramchester;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.integration.cloud.ConfigFromInstanceUserData;
import com.tramchester.integration.cloud.FetchInstanceMetadata;
import com.tramchester.integration.cloud.SendMetricsToCloudWatch;
import com.tramchester.integration.cloud.SignalToCloudformationReady;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.dataimport.ErrorCount;
import com.tramchester.integration.dataimport.FetchDataFromUrl;
import com.tramchester.integration.dataimport.TransportDataImporter;
import com.tramchester.integration.dataimport.TransportDataReader;
import com.tramchester.integration.dataimport.datacleanse.DataCleanser;
import com.tramchester.integration.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.CreateQueryTimes;
import com.tramchester.domain.ProvidesNotes;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.integration.graph.*;
import com.tramchester.integration.graph.Nodes.NodeFactory;
import com.tramchester.integration.graph.Relationships.PathToTransportRelationship;
import com.tramchester.integration.graph.Relationships.RelationshipFactory;
import com.tramchester.integration.mappers.TramJourneyResponseMapper;
import com.tramchester.integration.repository.RoutesRepository;
import com.tramchester.integration.repository.TransportDataFromFiles;
import com.tramchester.integration.resources.*;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import com.tramchester.services.StationLocalityService;
import org.apache.commons.io.FileUtils;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Dependencies {

    public static final String TFGM_UNZIP_DIR = "gtdf-out";
    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);

    public void initialise(TramchesterConfig configuration) throws IOException {
        Path dataPath = configuration.getDataPath();
        if (configuration.getPullData()) {
            logger.info("Pulling data");

            FetchDataFromUrl fetcher = new FetchDataFromUrl(dataPath, configuration.getTramDataUrl());
            fetcher.fetchData();
        }

        if (configuration.getFilterData() || configuration.getPullData()) {
            cleanseData(configuration.getAgencies(), dataPath, dataPath);
        }

        logger.info("Creating dependencies");
        // caching is on by default
        picoContainer.addComponent(TramchesterConfig.class, configuration);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(ClosedStations.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(CreateQueryTimes.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(NodeFactory.class);
        picoContainer.addComponent(RelationshipFactory.class);
        picoContainer.addComponent(ServiceHeuristics.class);
        picoContainer.addComponent(CachingCostEvaluator.class);
        picoContainer.addComponent(TimeBasedPathExpander.class);
        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(StationLocalityService.class);
        picoContainer.addComponent(ProvidesNotes.class);
        picoContainer.addComponent(TramJourneyResponseMapper.class);

        picoContainer.addComponent(RouteCodeToClassMapper.class);
        picoContainer.addComponent(UpdateRecentJourneys.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        picoContainer.addComponent(objectMapper);

        TransportDataReader dataReader = new TransportDataReader(dataPath);
        TransportDataImporter transportDataImporter = new TransportDataImporter(dataReader);

        picoContainer.addComponent(TransportDataFromFiles.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(DateTimeService.class);
        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);
        picoContainer.addComponent(MapTransportRelationshipsToStages.class);
        picoContainer.addComponent(PathToTransportRelationship.class);
        picoContainer.addComponent(MapPathToStages.class);
        picoContainer.addComponent(LocationToLocationJourneyPlanner.class);
        picoContainer.addComponent(SendMetricsToCloudWatch.class);
        picoContainer.addComponent(SpatialDatabaseService.class);
        picoContainer.addComponent(TransportGraphAddWalkingRoutes.class);
        picoContainer.addComponent(FeedInfoResource.class);
        picoContainer.addComponent(RoutesRepository.class);
        picoContainer.addComponent(RouteResource.class);

        rebuildGraph(configuration);

        picoContainer.addComponent(GraphHealthCheck.class);

        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = picoContainer.getComponent(SignalToCloudformationReady.class);
        signaller.send();
    }

    public ErrorCount cleanseData(Set<String> agencies, Path inputPath, Path outputPath) throws IOException {
        Path inputDir = inputPath.resolve(TFGM_UNZIP_DIR);
        TransportDataReader reader = new TransportDataReader(inputDir);
        TransportDataWriterFactory writerFactory = new TransportDataWriterFactory(outputPath);

        ErrorCount count = new ErrorCount();
        DataCleanser dataCleanser = new DataCleanser(reader, writerFactory, count);
        dataCleanser.run(agencies);
        if (!count.noErrors()) {
            logger.warn("Errors encounted during parsing data " + count);
        }
        logger.info("Data cleansing finished");
        return count;
    }

    private void rebuildGraph(TramchesterConfig configuration) throws IOException {
        String graphName = configuration.getGraphName();
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();

        File graphFile = new File(graphName);

        if (configuration.getRebuildGraph()) {
            logger.info("Deleting previous graph db for " + graphFile.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException e) {
                logger.error("Error deleting the graph!",e);
                throw e;
            }
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphFile));
            picoContainer.getComponent(TransportGraphBuilder.class).buildGraph();
            logger.info("Graph rebuild is finished for " + graphName);
        } else {
            logger.info("Not rebuilding graph " + graphFile.getAbsolutePath() + ". Loading graph");
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphFile));
        }

        if (configuration.getCreateLocality()) {
            picoContainer.getComponent(StationLocalityService.class).populateLocality();
        }
        logger.info("graph db ready for " + graphFile.getAbsolutePath());

        if (configuration.getAddWalkingRoutes()) {
            picoContainer.getComponent(TransportGraphAddWalkingRoutes.class).addCityCentreWalkingRoutes();
        }

    }

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

    public void close() {
        GraphDatabaseService graphService = picoContainer.getComponent(GraphDatabaseService.class);
        if (graphService==null) {
                logger.error("Unable to obtain GraphDatabaseService for shutdown");
        } else {
            if (graphService.isAvailable(1)) {
                logger.info("Shutting down graphDB");
                graphService.shutdown();
            }
        }
    }
}
