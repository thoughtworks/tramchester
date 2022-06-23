package com.tramchester.integration.dataimport;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.loader.*;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Peformance testing only")
public class GTFSStopTimeLoaderTest {

    private static IntegrationTramTestConfig config;
    private static GuiceContainerDependencies componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    public void shouldDoMultipleLoadsForPerformanceTestingOnly() {
        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);

        WriteableTransportData container = new TransportDataContainer(providesNow, "tfgm");

        TransportDataSourceFactory dataSourceFactory = componentContainer.get(TransportDataSourceFactory.class);
        TransportDataReaderFactory transportDataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);


        for (int i = 0; i < 100; i++) {
            TransportDataSource dataSource = dataSourceFactory.iterator().next();
            load(dataSource, container);
            dataSourceFactory.stop();
            transportDataReaderFactory.stop();
            transportDataReaderFactory.start();
            dataSourceFactory.start();
        }

        dataSourceFactory.stop();
    }

    private void load(TransportDataSource dataSource, WriteableTransportData writeableTransportData) {

        DataSourceInfo dataSourceInfo = dataSource.getDataSourceInfo();
        GTFSSourceConfig sourceConfig = dataSource.getConfig();

        TransportEntityFactory entityFactory = dataSource.getEntityFactory();

        // create loaders
        StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, config);
        AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSourceInfo, entityFactory);
        RouteDataLoader routeDataLoader = new RouteDataLoader(writeableTransportData, sourceConfig, entityFactory);
        TripLoader tripLoader = new TripLoader(writeableTransportData, entityFactory);
        GTFSStopTimeLoader stopTimeLoader = new GTFSStopTimeLoader(writeableTransportData, entityFactory, sourceConfig);

        PreloadedStationsAndPlatforms interimStations = stopDataLoader.load(dataSource.getStops());
        CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());
        RouteDataLoader.ExcludedRoutes excludedRoutes = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);

        interimAgencies.clear();

        TripAndServices interimTripsAndServices = tripLoader.load(dataSource.getTrips(), excludedRoutes);
        stopTimeLoader.load(dataSource.getStopTimes(), interimStations, interimTripsAndServices);

        excludedRoutes.clear();
        interimStations.clear();

        interimTripsAndServices.clear();

        dataSource.closeAll();

    }
}
