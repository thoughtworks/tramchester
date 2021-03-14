package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.dataimport.data.TransportDataStreams;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.graphbuild.*;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GraphBuildAndStartTest {

    // spin up graph, primarily here to diagnose out of memory issues, isolate just the graph build
    //@Disabled("for diagnostics only")
    @Test
    void shouldBuildGraphAndStart() throws IOException {
        TramchesterConfig config =new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        Unzipper unzipper = new Unzipper();
        FetchDataFromUrl fetcher = new FetchDataFromUrl(unzipper, new URLDownloadAndModTime(), config);
        fetcher.start();

        ProvidesNow providesNow = new ProvidesLocalNow();

        NodeTypeRepository nodeTypeRepository = new NodeIdLabelMap();

        CsvMapper mapper = CsvMapper.builder().build();

        FetchFileModTime fetchFileModTime = new FetchFileModTime();

        TransportDataLoader dataLoader = new TransportDataLoaderFiles(config, fetchFileModTime, mapper);
        TransportDataStreams dataStreams = new TransportDataStreams(dataLoader, config, fetcher.getReady());
        TransportDataFromFiles dataFromFiles = new TransportDataFromFiles(dataStreams, config, providesNow);

        TransportData transportData = dataFromFiles.getData();
        GraphDatabase graphDatabase = new GraphDatabase(config, transportData);

        IncludeAllFilter graphFilter = new IncludeAllFilter();

        GraphBuilderCache builderCache = new GraphBuilderCache();
        StationsAndLinksGraphBuilder stationAndLinksBuilder = new StationsAndLinksGraphBuilder(graphDatabase, config, graphFilter, nodeTypeRepository,
                transportData, builderCache);

        StationsAndLinksGraphBuilder.Ready stationAndLinksBuilderReady = stationAndLinksBuilder.getReady();
        FindStationsByNumberLinks findStationsByNumberConnections = new FindStationsByNumberLinks(graphDatabase, stationAndLinksBuilderReady);
        InterchangeRepository interchangeRepository = new InterchangeRepository(findStationsByNumberConnections, transportData, config);

        StagedTransportGraphBuilder stagedTransportGraphBuilder = new StagedTransportGraphBuilder(graphDatabase, config, graphFilter,
                nodeTypeRepository, transportData, interchangeRepository, builderCache, stationAndLinksBuilderReady);

        StagedTransportGraphBuilder.Ready graphBuilderReady = stagedTransportGraphBuilder.getReady();
        assertNotNull(graphBuilderReady);

        ////////

        graphDatabase.start();

        interchangeRepository.start();
        Assertions.assertTrue(graphDatabase.isAvailable(2000));
        stationAndLinksBuilder.start();
        stagedTransportGraphBuilder.start();

        graphDatabase.stop();
        interchangeRepository.dispose();

        TestEnv.deleteDBIfPresent(config);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("test_and_start.db");
        }

    }
}
