package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeIdQuery;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class GraphBuildAndStartTest {

    // spin up graph, primarily here to diagnose out of memory issues, isloate just the graph build

    @Test
    public void shouldBuildGraphAndStart() throws IOException {
        TramchesterConfig config =new SubgraphConfig();
        File graphFile = new File(config.getGraphName());
        if (graphFile.exists()) {
            FileUtils.deleteDirectory(graphFile);
        }

        FetchDataFromUrl fetcher = new FetchDataFromUrl(new URLDownloader(), config);
        Unzipper unzipper = new Unzipper();
        fetcher.fetchData(unzipper);
        DataCleanser dataCleaner = new DataCleanser(new TransportDataReaderFactory(config), new TransportDataWriterFactory(config), config);
        dataCleaner.run();

        TransportDataImporter dataImporter = new TransportDataImporter(new TransportDataReaderFactory(config));

        GraphDatabaseService graphDbService = Dependencies.createGraphDatabaseService(graphFile);
        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
        SpatialDatabaseService spacialDatabaseService = new SpatialDatabaseService(graphDbService);
        GraphQuery graphQuery = new GraphQuery(graphDbService, spacialDatabaseService);
        NodeIdQuery nodeIdQuery = new NodeIdQuery(graphDbService, graphQuery, config);
        TransportDataSource transportData = dataImporter.load();

        InterchangeRepository interchangeRepository = new InterchangeRepository(transportData, config);
        TransportGraphBuilder transportGraphBuilder = new TransportGraphBuilder(graphDbService, transportData,
                nodeIdLabelMap, nodeIdQuery, interchangeRepository);

        assertTrue("built graph ok", transportGraphBuilder.buildGraph());
        assertTrue(graphDbService.isAvailable(2000));

        graphDbService.shutdown();

    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("test_and_start.db");
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }
}
