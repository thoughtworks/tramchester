package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.postcodes.PostCodeDatasourceConfig;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    private final GraphDBTestConfig dbConfig;
    protected final RailRemoteDataSourceConfig railRemoteDataSource;

//    public static final StationClosures piccGardensClosed = new StationClosuresConfig(
//            Collections.singleton(PiccadillyGardens.getId()),
//            LocalDate.of(2022,10,23),
//            LocalDate.of(2022,11,29), false);

    public static final List<StationClosures> CurrentClosures = Collections.emptyList();

    protected IntegrationTestConfig(GraphDBTestConfig dbConfig) {
        final Path naptanLocalDataPath = Path.of("data/naptan");
        remoteNaptanXMLConfig = new NaptanRemoteDataSourceConfig(naptanLocalDataPath);
        remoteNPTGconfig = new NPTGDataSourceTestConfig();
        postCodeDatasourceConfig = new PostCodeDatasourceConfig();
        railRemoteDataSource = new RailRemoteDataSourceConfig("data/rail");

        this.dbConfig = dbConfig;
    }

    @Override
    public GraphDBConfig getGraphDBConfig() {
        return dbConfig;
    }
}
