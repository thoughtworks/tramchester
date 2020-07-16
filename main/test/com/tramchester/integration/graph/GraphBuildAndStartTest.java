package com.tramchester.integration.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.*;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportDataFromFilesBuilder;
import com.tramchester.repository.TransportDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class GraphBuildAndStartTest {

    // spin up graph, primarily here to diagnose out of memory issues, isolate just the graph build

    @Test
    void shouldBuildGraphAndStart() throws IOException {
        TramchesterConfig config =new SubgraphConfig();
        File graphFile = new File(config.getGraphName());
        if (graphFile.exists()) {
            FileUtils.deleteDirectory(graphFile);
        }

        FetchDataFromUrl fetcher = new FetchDataFromUrl(new URLDownloader(), config);
        Unzipper unzipper = new Unzipper();
        fetcher.fetchData(unzipper);
        ProvidesNow providesNow = new ProvidesLocalNow();
        DataCleanser dataCleaner = new DataCleanser(new TransportDataReaderFactory(config), new TransportDataWriterFactory(config),
                providesNow, config);
        dataCleaner.run();

        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
        CoordinateTransforms coordinateTransforms = new CoordinateTransforms();
        StationLocations stationLocations = new StationLocations(coordinateTransforms);
        TransportDataBuilderFactory fileFactory = new TransportDataBuilderFactory(new TransportDataReaderFactory(config),
                providesNow, stationLocations);
        TransportDataFromFilesBuilder builder = fileFactory.create();

        builder.load();
        TransportDataSource transportData = builder.getData();
        InterchangeRepository interchangeRepository = new InterchangeRepository(transportData, config);

        GraphDatabase graphDatabase = new GraphDatabase(config);
        GraphQuery graphQuery = new GraphQuery(graphDatabase);

        GraphBuilder graphBuilder = new StagedTransportGraphBuilder(graphDatabase, config, new IncludeAllFilter(),
                graphQuery, nodeIdLabelMap, transportData, interchangeRepository);

        interchangeRepository.start();
        graphDatabase.start();
        Assertions.assertTrue(graphDatabase.isAvailable(2000));
        graphBuilder.start();

        graphDatabase.stop();
        interchangeRepository.dispose();
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
