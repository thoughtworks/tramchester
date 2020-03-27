package com.tramchester.integration.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.graph.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;

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

        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
        TransportDataImporter dataImporter = new TransportDataImporter(new TransportDataReaderFactory(config));
        TransportDataSource transportData = dataImporter.load();
        InterchangeRepository interchangeRepository = new InterchangeRepository(transportData, config);

        GraphDatabase graphDatabase = new GraphDatabase(config);
        GraphQuery graphQuery = new GraphQuery(graphDatabase);
        NodeIdQuery nodeIdQuery = new NodeIdQuery(graphQuery, config);

        TransportGraphBuilder transportGraphBuilder = new TransportGraphBuilder(graphDatabase, new IncludeAllFilter(), transportData,
                nodeIdLabelMap, nodeIdQuery, interchangeRepository, config);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(2000));
        transportGraphBuilder.start();
        graphDatabase.stop();
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
