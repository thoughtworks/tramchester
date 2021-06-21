package com.tramchester.unit.graph.calculation;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;

public class SimpleGraphConfig extends IntegrationTestConfig {

    public SimpleGraphConfig(String dbFilename) {
        super(new GraphDBTestConfig("unitTest", dbFilename));
    }

    @Override
    protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
        TFGMGTFSSourceTestConfig tfgmTestDataSourceConfig = new TFGMGTFSSourceTestConfig("data/tram",
                GTFSTransportationType.tram, Tram, Collections.emptySet(), Collections.emptySet());
        return Collections.singletonList(tfgmTestDataSourceConfig);
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.singletonList(new NaptanRemoteDataSourceConfig("data/naptan"));
    }

    @Override
    public int getNumberQueries() {
        return 1;
    }

    @Override
    public int getQueryInterval() {
        return 6;
    }

    @Override
    public Path getCacheFolder() {
        return Path.of("testData/cache/unit");
    }
}
