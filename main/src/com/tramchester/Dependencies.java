package com.tramchester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.cloud.*;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StationDTOFactory;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.graph.*;
import com.tramchester.healthchecks.*;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.mappers.*;
import com.tramchester.repository.*;
import com.tramchester.resources.*;
import com.tramchester.services.SpatialService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
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
import java.util.List;

import static java.lang.String.format;

public class Dependencies {
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);

    private final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private final GraphFilter graphFilter;

    public Dependencies() {
        graphFilter=null;
    }

    public Dependencies(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
    }

    public void initialise(TramchesterConfig configuration) throws IOException {
        // caching is on by default
        picoContainer.addComponent(TramchesterConfig.class, configuration);

        // only needed if loading data?
        picoContainer.addComponent(TransportDataReaderFactory.class);
        picoContainer.addComponent(TransportDataWriterFactory.class);
        picoContainer.addComponent(DataCleanser.class);
        picoContainer.addComponent(URLDownloader.class);
        picoContainer.addComponent(FetchDataFromUrl.class);
        picoContainer.addComponent(Unzipper.class);
        picoContainer.addComponent(TransportDataImporter.class);

        FetchDataFromUrl fetcher = get(FetchDataFromUrl.class);
        Unzipper unzipper = get(Unzipper.class);
        fetcher.fetchData(unzipper);
        cleanseData();

        TransportDataImporter transportDataImporter = get(TransportDataImporter.class);
        TransportDataSource transportData = transportDataImporter.load();

        initialise(configuration, transportData);
    }

    // init dependencies but possibly with alternative source of transport data
    public void initialise(TramchesterConfig configuration, TransportDataSource transportData) throws IOException {
        logger.info("Creating dependencies");

        // caching is on by default
        if (picoContainer.getComponent(TramchesterConfig.class)==null) {
            picoContainer.addComponent(TramchesterConfig.class, configuration);
        }
        picoContainer.addComponent(TransportDataSource.class, transportData);

        picoContainer.addComponent(FileModTime.class);
        picoContainer.addComponent(VersionRepository.class);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(DeparturesResource.class);
        picoContainer.addComponent(DeparturesMapper.class);
        picoContainer.addComponent(ClosedStations.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(CreateQueryTimes.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(TramPositionsResource.class);
        picoContainer.addComponent(ServiceHeuristics.class);

        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(RouteCalculatorArriveBy.class);
        picoContainer.addComponent(NodeIdQuery.class);
        picoContainer.addComponent(ProvidesNotes.class);
        picoContainer.addComponent(JourneysMapper.class);
        picoContainer.addComponent(TramJourneyToDTOMapper.class);
        picoContainer.addComponent(RouteCodeToClassMapper.class);
        picoContainer.addComponent(UpdateRecentJourneys.class);
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);
        picoContainer.addComponent(MapPathToStages.class);
        picoContainer.addComponent(LocationJourneyPlanner.class);
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
        picoContainer.addComponent(MyLocationFactory.class);
        picoContainer.addComponent(RouteReachable.class);
        picoContainer.addComponent(TramReachabilityRepository.class);
        picoContainer.addComponent(NodeIdLabelMap.class);
        picoContainer.addComponent(GraphQuery.class);
        picoContainer.addComponent(StationAdjacenyRepository.class);
        picoContainer.addComponent(new ObjectMapper());
        picoContainer.addComponent(StageDTOFactory.class);
        picoContainer.addComponent(JourneyDTOFactory.class);
        picoContainer.addComponent(StationDTOFactory.class);
        picoContainer.addComponent(HeadsignMapper.class);
        picoContainer.addComponent(TramPositionInference.class);
        picoContainer.addComponent(ProvidesLocalNow.class);
        picoContainer.addComponent(GraphHealthCheck.class);
        picoContainer.addComponent(DataExpiryHealthCheck.class);
        picoContainer.addComponent(LiveDataHealthCheck.class);
        picoContainer.addComponent(NewDataAvailableHealthCheck.class);
        picoContainer.addComponent(LiveDataMessagesHealthCheck.class);
        picoContainer.addComponent(InterchangeRepository.class);

        createGraph(configuration);

        TramReachabilityRepository tramReachabilityRepository = get(TramReachabilityRepository.class);
        tramReachabilityRepository.buildRepository();
    }

    private void cleanseData() throws IOException {
        ErrorCount count = get(DataCleanser.class).run();

        if (!count.noErrors()) {
            logger.warn("Errors encounted during parsing data " + count);
        }
        logger.info("Data cleansing finished");
    }

    private void createGraph(TramchesterConfig configuration) throws IOException {
        String graphName = configuration.getGraphName();
        logger.info("Create or load graph " + graphName);
        File graphFile = new File(graphName);

        boolean rebuildGraph = configuration.getRebuildGraph();

        if (rebuildGraph) {
            logger.info("Deleting previous graph db for " + graphFile.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException ioException) {
                logger.error("Error deleting the graph!", ioException);
                throw ioException;
            }
        }
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory().setUserLogProvider(new Slf4jLogProvider());

        GraphDatabaseBuilder builder = graphDatabaseFactory.
                newEmbeddedDatabaseBuilder(graphFile).
                loadPropertiesFromFile("config/neo4j.conf");

        GraphDatabaseService graphDatabaseService = builder.newGraphDatabase();
        if (!graphDatabaseService.isAvailable(1000)) {
            logger.error("DB Service is not available");
        }
        picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseService);

        picoContainer.start();

        if (rebuildGraph) {
            logger.info("Rebuild of graph DB for " + graphName);
            TransportGraphBuilder graphBuilder = picoContainer.getComponent(TransportGraphBuilder.class);
            if (graphFilter==null) {
                graphBuilder.buildGraph();
            } else {
                graphBuilder.buildGraphwithFilter(graphFilter);
            }
            logger.info("Graph rebuild is finished for " + graphName);
        } else {
            logger.info("Load existing graph");
            populateNodeLabelMap(graphDatabaseService);
        }

//        if (configuration.getCreateLocality()) {
//            picoContainer.getComponent(StationLocalityService.class).populateLocality();
//        }
        logger.info("graph db ready for " + graphFile.getAbsolutePath());

    }

    private void populateNodeLabelMap(GraphDatabaseService graphDatabaseService) {
        logger.info("Rebuilding node->label index");
        NodeIdLabelMap nodeIdLabelMap = get(NodeIdLabelMap.class);
        TransportGraphBuilder.Labels[] labels = TransportGraphBuilder.Labels.values();
        try (Transaction tx = graphDatabaseService.beginTx()) {
            for (TransportGraphBuilder.Labels label : labels) {
                graphDatabaseService.findNodes(label).stream().forEach(node -> nodeIdLabelMap.put(node.getId(), label));
            }
            tx.success();
        }
        nodeIdLabelMap.freeze();
    }

    public <T> T get(Class<T> klass) {

        T component = picoContainer.getComponent(klass);
        if (component==null) {
            logger.warn("Missing dependency " + klass);
        }
        return component;
    }

    public void close() {
        logger.info("Dependencies close");
        shutdownGraphDB();
        logger.info("Begin cache stats");
        List<ReportsCacheStats> components = picoContainer.getComponents(ReportsCacheStats.class);
        components.forEach(component -> reportCacheStats(component.getClass().getSimpleName(), component.stats()));
        logger.info("End cache stats");
        logger.info("Dependencies closed");
        picoContainer.stop();
        picoContainer.dispose();
    }

    private void reportCacheStats(String className, List<Pair<String, CacheStats>> stats) {
        stats.forEach(stat -> logger.info(format("%s: %s: %s", className, stat.getLeft(), stat.getRight().toString())));
    }

    private void shutdownGraphDB() {
        try {
            GraphDatabaseService graphService = picoContainer.getComponent(GraphDatabaseService.class);
                if (graphService==null) {
                        logger.error("Unable to obtain GraphDatabaseService for shutdown");
                } else {
                    if (graphService.isAvailable(1000)) {
                        logger.info("Shutting down graphDB");
                        graphService.shutdown();
                    } else {
                        logger.warn("Graph reported unavailable, attempt shutdown anyway");
                        graphService.shutdown();
                    }
                }
        } catch (Exception exceptionInClose) {
            logger.error("Exception during close down", exceptionInClose);
        }
    }

    public TramchesterConfig getConfig() {
        return get(TramchesterConfig.class);
    }
}
