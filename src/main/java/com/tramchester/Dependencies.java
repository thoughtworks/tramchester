package com.tramchester;


import com.tramchester.config.AppConfiguration;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.domain.TransportData;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportGraphBuilder;
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

    public void initialise(AppConfiguration configuration) throws IOException {

        logger.info("Creating dependencies");
        picoContainer.addComponent(AppConfiguration.class, configuration);

        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(RouteCalculator.class);
        TransportDataImporter transportDataImporter = new TransportDataImporter(PATH);
        picoContainer.addComponent(TransportData.class, transportDataImporter.load());
        picoContainer.addComponent(TransportGraphBuilder.class);
        picoContainer.addComponent(SpatialService.class);
        picoContainer.addComponent(DateTimeService.class);

        rebuildGraph(configuration);
    }

    private void rebuildGraph(AppConfiguration configuration) throws IOException {
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
            picoContainer.addComponent(GraphDatabaseService.class, new GraphDatabaseFactory().newEmbeddedDatabase(GRAPH_NAME));
        }
    }

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

}
