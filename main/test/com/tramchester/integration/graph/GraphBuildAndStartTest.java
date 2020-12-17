package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class GraphBuildAndStartTest {

    // spin up graph, primarily here to diagnose out of memory issues, isolate just the graph build
    @Disabled("for diagnostics only")
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

        CsvMapper mapper = CsvMapper.builder().build();

        FetchFileModTime fetchFileModTime = new FetchFileModTime();
        TransportDataFromFilesBuilder fileFactory = new TransportDataFromFilesBuilder(new TransportDataReaderFactory(config, fetchFileModTime, mapper),
                providesNow, config);
        TransportDataFromFiles builder = fileFactory.create();

        //StationLocations stationLocations = new StationLocations(builder);
        //builder.load();
        TransportData transportData = builder.getData();
        InterchangeRepository interchangeRepository = new InterchangeRepository(transportData, config);

        IncludeAllFilter graphFilter = new IncludeAllFilter();
        GraphDatabase graphDatabase = new GraphDatabase(config, transportData);

        StagedTransportGraphBuilder graphBuilder = new StagedTransportGraphBuilder(graphDatabase, config, graphFilter,
                nodeIdLabelMap, transportData, interchangeRepository);

        GraphBuilder.Ready ready = graphBuilder.getReady();
        GraphQuery graphQuery = new GraphQuery(graphDatabase, ready);

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
