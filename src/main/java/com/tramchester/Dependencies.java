package com.tramchester;


import com.tramchester.config.AppConfiguration;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.domain.TransportData;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.resources.JourneyPlannerResource;
import com.tramchester.resources.StationResource;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.FileUtils;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Dependencies {
    public static final String GRAPH_NAME = "tramchester.db";
    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);
    private static String PATH = "data/tram/";

    public void initialise(TramchesterConfig configuration) throws Exception {
        if (configuration.isPullData()) {
            logger.info("Pulling and cleansing data");
            new DataCleanser().main(null);
        }
        logger.info("Creating dependencies");
        picoContainer.addComponent(TramchesterConfig.class, configuration);

        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(JourneyResponseMapper.class);

        TransportDataReader dataReader = new TransportDataReader(PATH);
        TransportDataImporter transportDataImporter = new TransportDataImporter(dataReader);
        picoContainer.addComponent(TransportData.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(DateTimeService.class);

        rebuildGraph(configuration);
    }

    private void rebuildGraph(TramchesterConfig configuration) throws IOException {
        if (configuration.isRebuildGraph()) {
            logger.info("Deleting previous graph db for " + GRAPH_NAME);
            try {
                FileUtils.deleteRecursively(new File(GRAPH_NAME));
            } catch (IOException e) {
                logger.error("Error deleting the graph!");
                throw e;
            }
            picoContainer.addComponent(GraphDatabaseService.class, new GraphDatabaseFactory().newEmbeddedDatabase(GRAPH_NAME));
            picoContainer.getComponent(TransportGraphBuilder.class).buildGraph();
        } else {
            logger.warn("Not rebuilding graph");
            picoContainer.addComponent(GraphDatabaseService.class, new GraphDatabaseFactory().newEmbeddedDatabase(GRAPH_NAME));
        }
    }

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

}
