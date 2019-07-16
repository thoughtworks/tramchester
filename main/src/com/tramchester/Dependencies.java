package com.tramchester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.cloud.*;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.ErrorCount;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.CreateQueryTimes;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.graph.*;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.healthchecks.*;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.*;
import com.tramchester.repository.*;
import com.tramchester.resources.*;
import com.tramchester.services.SpatialService;
import com.tramchester.services.StationLocalityService;
import org.apache.commons.io.FileUtils;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class Dependencies {
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);

    public static final String TFGM_UNZIP_DIR = "gtdf-out";
    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private final GraphFilter graphFilter;

    public Dependencies() {
        graphFilter=null;
    }

    public Dependencies(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
    }


    public void initialise(TramchesterConfig configuration) throws IOException {
        Path dataPath = configuration.getDataPath();

        FetchDataFromUrl fetcher = new FetchDataFromUrl(dataPath, configuration.getTramDataUrl());
        picoContainer.addComponent(FetchDataFromUrl.class, fetcher);

        if (configuration.getPullData()) {
            logger.info("Pulling data");
            fetcher.fetchData();
        }

        cleanseData(dataPath, dataPath, configuration);

        logger.info("Creating dependencies");
        // caching is on by default
        picoContainer.addComponent(TramchesterConfig.class, configuration);
        picoContainer.addComponent(VersionRepository.class);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(DeparturesResource.class);
        picoContainer.addComponent(DeparturesMapper.class);
        picoContainer.addComponent(ClosedStations.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(CreateQueryTimes.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(NodeFactory.class);
        picoContainer.addComponent(RelationshipFactory.class);
        picoContainer.addComponent(ServiceHeuristics.class);
        picoContainer.addComponent(CachingCostEvaluator.class);
        picoContainer.addComponent(LazyTimeBasedPathExpander.class);
        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(StationLocalityService.class);
        picoContainer.addComponent(ProvidesNotes.class);
        picoContainer.addComponent(JourneysMapper.class);

        if (configuration.getEdgePerTrip()) {
            picoContainer.addComponent(TramJourneyResponseWithTimesMapper.class);
        } else {
            picoContainer.addComponent(TramJourneyResponseMapper.class);
        }

        picoContainer.addComponent(RouteCodeToClassMapper.class);
        picoContainer.addComponent(UpdateRecentJourneys.class);

        ObjectMapper objectMapper = new ObjectMapper();
        picoContainer.addComponent(objectMapper);

        TransportDataReader dataReader = new TransportDataReader(dataPath, false);
        TransportDataImporter transportDataImporter = new TransportDataImporter(dataReader);

        picoContainer.addComponent(TransportDataFromFiles.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);
        picoContainer.addComponent(MapTransportRelationshipsToStages.class);
        picoContainer.addComponent(PathToTransportRelationship.class);
        picoContainer.addComponent(MapPathToStages.class);
        picoContainer.addComponent(LocationToLocationJourneyPlanner.class);
        picoContainer.addComponent(SendMetricsToCloudWatch.class);
        picoContainer.addComponent(SpatialDatabaseService.class);
        picoContainer.addComponent(FeedInfoResource.class);
        picoContainer.addComponent(RoutesRepository.class);
        picoContainer.addComponent(RouteResource.class);
        picoContainer.addComponent(AreaResource.class);
        picoContainer.addComponent(LiveDataHTTPFetcher.class);
        picoContainer.addComponent(LiveDataParser.class);
        picoContainer.addComponent(LiveDataRepository.class);
        picoContainer.addComponent(ClientForS3.class);
        picoContainer.addComponent(UploadsLiveData.class);
        picoContainer.addComponent(CachedNodeOperations.class);
        picoContainer.addComponent(LatestFeedInfoRepository.class);

        rebuildGraph(configuration);

        picoContainer.addComponent(ProvidesNow.class, new ProvidesNow() {
            @Override
            public TramTime getNow() {
                return TramTime.of(ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalTime());
            }

            @Override
            public LocalDate getDate() {
                return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDate();
            }
        });
        picoContainer.addComponent(GraphHealthCheck.class);
        picoContainer.addComponent(DataExpiryHealthCheck.class);
        picoContainer.addComponent(LiveDataHealthCheck.class);
        picoContainer.addComponent(NewDataAvailableHealthCheck.class);
        picoContainer.addComponent(LiveDataMessagesHealthCheck.class);

    }

    public void cleanseData(Path inputPath, Path outputPath, TramchesterConfig config) throws IOException {
        Path inputDir = inputPath.resolve(TFGM_UNZIP_DIR);
        TransportDataReader reader = new TransportDataReader(inputDir, true);
        TransportDataWriterFactory writerFactory = new TransportDataWriterFactory(outputPath);

        ErrorCount count = new ErrorCount();
        DataCleanser dataCleanser = new DataCleanser(reader, writerFactory, count, config);
        dataCleanser.run(config.getAgencies());
        if (!count.noErrors()) {
            logger.warn("Errors encounted during parsing data " + count);
        }
        logger.info("Data cleansing finished");
    }

    private void rebuildGraph(TramchesterConfig configuration) throws IOException {
        String graphName = configuration.getGraphName();
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory().setUserLogProvider(new Slf4jLogProvider());

        File graphFile = new File(graphName);
        GraphDatabaseBuilder builder = graphDatabaseFactory.
                newEmbeddedDatabaseBuilder(graphFile).
                loadPropertiesFromFile("config/neo4j.conf");

        if (configuration.getRebuildGraph()) {
            logger.info("Deleting previous graph db for " + graphFile.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException e) {
                logger.error("Error deleting the graph!",e);
                throw e;
            }
            picoContainer.addComponent(GraphDatabaseService.class, builder.newGraphDatabase());

            TransportGraphBuilder graphBuilder = picoContainer.getComponent(TransportGraphBuilder.class);
            if (graphFilter==null) {
                graphBuilder.buildGraph();
            } else {
                graphBuilder.buildGraphwithFilter(graphFilter);
            }
            logger.info("Graph rebuild is finished for " + graphName);
        } else {
            logger.info("Not rebuilding graph " + graphFile.getAbsolutePath() + ". Loading graph");
            picoContainer.addComponent(GraphDatabaseService.class, builder.newGraphDatabase());
        }

        if (configuration.getCreateLocality()) {
            picoContainer.getComponent(StationLocalityService.class).populateLocality();
        }
        logger.info("graph db ready for " + graphFile.getAbsolutePath());

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
