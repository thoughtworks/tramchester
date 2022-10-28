package com.tramchester.integration.testSupport;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.rail.RailRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.reference.TramStations;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.tramchester.domain.reference.CentralZoneStation.*;

public abstract class IntegrationTestConfig extends TestConfig {

    protected final NaptanRemoteDataSourceConfig remoteNaptanXMLConfig;
    protected final PostCodeDatasourceConfig postCodeDatasourceConfig;
    protected final RemoteDataSourceConfig remoteNPTGconfig;

    private final GraphDBTestConfig dbConfig;
    protected final RailRemoteDataSourceConfig railRemoteDataSource;

    public static final TramDate VictoriaClosureDate = TramDate.of(2022,11,6);

    private static final StationClosures piccGardensClosed = new StationClosuresConfig(
            Collections.singleton(PiccadillyGardens.getId()),
            LocalDate.of(2022,10,23),
            LocalDate.of(2022,11,30), false);

    private static final StationClosures victoriaClosed = new StationClosuresConfig(
            new HashSet<>(Arrays.asList(Victoria.getId(), MarketStreet.getId(), Shudehill.getId())),
            VictoriaClosureDate.toLocalDate(),
            VictoriaClosureDate.toLocalDate(), true);

    public static final List<StationClosures> CurrentClosures = Arrays.asList(piccGardensClosed, victoriaClosed);

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
