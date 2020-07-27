package com.tramchester.integration.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFilesBuilderGeoFilter;
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

        FetchDataFromUrl fetcher = new FetchDataFromUrl(new URLDownloadAndModTime(), config);
        Unzipper unzipper = new Unzipper();
        fetcher.fetchData(unzipper);
        ProvidesNow providesNow = new ProvidesLocalNow();

        NodeIdLabelMap nodeIdLabelMap = new NodeIdLabelMap();
//        CoordinateTransforms coordinateTransforms = new CoordinateTransforms();
        StationLocations stationLocations = new StationLocations();
        FetchFileModTime fetchFileModTime = new FetchFileModTime();
        TransportDataBuilderFactory fileFactory = new TransportDataBuilderFactory(new TransportDataReaderFactory(config, fetchFileModTime),
                providesNow, stationLocations, config);
        TransportDataFromFilesBuilderGeoFilter builder = fileFactory.create();

        builder.load();
        TransportData transportData = builder.getData();
        InterchangeRepository interchangeRepository = new InterchangeRepository(transportData, config);

        IncludeAllFilter graphFilter = new IncludeAllFilter();
        GraphDatabase graphDatabase = new GraphDatabase(config, transportData);
        GraphQuery graphQuery = new GraphQuery(graphDatabase);

        GraphBuilder graphBuilder = new StagedTransportGraphBuilder(graphDatabase, config, graphFilter,
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

    }
}
