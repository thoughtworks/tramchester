package com.tramchester;


import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.TransportDataFromFiles;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.resources.StationResource;
import com.tramchester.resources.VersionResource;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Dependencies {

    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);
    private static String PATH = "data/tram/";

    public void initialise(TramchesterConfig configuration) throws Exception {
        if (configuration.isPullData()) {
            logger.info("Pulling and cleansing data");
            // TODO REFACTOR TO INJECT DEPENDENCIES AND STOP CALLING MAIN
            DataCleanser.main(null);
        }
        logger.info("Creating dependencies");
        picoContainer.addComponent(TramchesterConfig.class, configuration);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(ClosedStations.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(JourneyResponseMapper.class);

        TransportDataReader dataReader = new TransportDataReader(PATH);
        TransportDataImporter transportDataImporter = new TransportDataImporter(dataReader);
        picoContainer.addComponent(TransportDataFromFiles.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(DateTimeService.class);
        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);

        rebuildGraph(configuration);

        logger.info("Prepare to signal cloud formation if running in cloud");
        SignalToCloudformationReady signaller = picoContainer.getComponent(SignalToCloudformationReady.class);
        signaller.send();
    }

    private void rebuildGraph(TramchesterConfig configuration) throws IOException {
        String graphName = configuration.getGraphName();
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();

        if (configuration.isRebuildGraph()) {
            logger.info("Deleting previous graph db for " + graphName);
            try {
                FileUtils.deleteDirectory(new File(graphName));
            } catch (IOException e) {
                logger.error("Error deleting the graph!",e);
                throw e;
            }
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphName));
            picoContainer.getComponent(TransportGraphBuilder.class).buildGraph();
            logger.info("Graph rebuild is finished for " + graphName);
        } else {
            logger.info("Not rebuilding graph " + graphName);
            picoContainer.addComponent(GraphDatabaseService.class, graphDatabaseFactory.newEmbeddedDatabase(graphName));
        }

    }

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

    public void close() {
        GraphDatabaseService graphService = picoContainer.getComponent(GraphDatabaseService.class);
        if (graphService==null) {
                logger.error("Unable to obtain GraphDatabaseService");
        } else {
            if (graphService.isAvailable(1)) {
                logger.info("Shutting down graphDB");
                graphService.shutdown();
            }
        }
    }
}
