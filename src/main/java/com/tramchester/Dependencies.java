package com.tramchester;


import com.tramchester.config.AppConfiguration;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.resources.TestResource;
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

    public void initialise(AppConfiguration configuration) {
//        logger.info("Deleting previous graph db for " + GRAPH_NAME);
//        try {
//            FileUtils.deleteRecursively(new File(GRAPH_NAME));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        picoContainer.addComponent(AppConfiguration.class, configuration);

        picoContainer.addComponent(TestResource.class);
        picoContainer.addComponent(GraphDatabaseService.class, new GraphDatabaseFactory().newEmbeddedDatabase(GRAPH_NAME));
        picoContainer.addComponent(RouteCalculator.class);

    }


    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

}
