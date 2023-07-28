package com.tramchester.unit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationIdPair;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMismatchTest {

    enum Category {
        Closures,
        Modes,
        Bounds;

        public boolean not(Collection<Category> excluded) {
            return !excluded.contains(this);
        }
    }

    @Test
    void shouldBeAbleToLoadAllConfigWithoutExceptions() throws IOException, ConfigurationException {
        // Note: this does not catch all the same validation cases as app start up
        @NotNull YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();
        Set<Path> configFiles = getConfigFiles();

        for (Path config : configFiles) {
            factory.build(config.toFile());
        }
    }

    @Test
    void shouldHaveValidIdsForRemoteConfigSections() throws IOException, ConfigurationException {
        Set<Path> configFiles = getConfigFiles();

        for(Path config : configFiles) {
            AppConfiguration configuration = loadConfigFromFile(config);
            List<RemoteDataSourceConfig> remoteSourceConfigs = configuration.getRemoteDataSourceConfig();
            for(RemoteDataSourceConfig remoteSourceConfig : remoteSourceConfigs) {
                final DataSourceID dataSourceID = DataSourceID.findOrUnknown(remoteSourceConfig.getName());
                assertNotEquals(DataSourceID.unknown, dataSourceID,
                        "Bad source id for " + remoteSourceConfig.getName() + " in " + config.toAbsolutePath());

            }
        }
    }

    @Test
    void shouldNotUsePrimitiveTypesInAppConfigAsDisablesNullChecking() {
        Field[] fields = AppConfiguration.class.getDeclaredFields();
        Set<String> nonPrim = Arrays.stream(fields).
                filter(field -> field.getAnnotation(JsonProperty.class) != null).
                filter(field -> field.getType().isPrimitive()).
                map(Field::getName).
                collect(Collectors.toSet());
        assertEquals(Collections.emptySet(), nonPrim);
    }

    @Test
    void shouldHaveKeyParametersSameForTramIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(true);

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = testConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(1, dataSourceConfig.size());

        assertRemoteSources(dataSourceConfig, testDataSourceConfig, 0);
    }

    @Test
    void shouldHaveKeyParametersSameForBusIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("buses.yml");
        IntegrationBusTestConfig testConfig = new IntegrationBusTestConfig();

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForRailntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("rail.yml");
        IntegrationRailTestConfig testConfig = new IntegrationRailTestConfig();

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);

        List<RemoteDataSourceConfig> remoteSources = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testRemoteSources = testConfig.getRemoteDataSourceConfig();

        assertEquals(remoteSources.size(), testRemoteSources.size());
        assertEquals(3, testRemoteSources.size());

        assertRemoteSources(remoteSources, testRemoteSources, 0);
        assertRemoteSources(remoteSources, testRemoteSources, 1);
        assertRemoteSources(remoteSources, testRemoteSources, 2);

        RailConfig rail = appConfig.getRailConfig();
        RailConfig testRail = appConfig.getRailConfig();

        assertEquals(rail.getStations(), testRail.getStations());
        assertEquals(rail.getTimetable(), testRail.getTimetable());
        assertEquals(rail.getModes(), testRail.getModes());

        assertRailLiveData(appConfig.getOpenldbwsConfig(), testConfig.getOpenldbwsConfig());

        checkDataSourceConfig(appConfig.getRailConfig(), testConfig.getRailConfig());

        checkRailDataVersionFor(appConfig);
        checkRailDataVersionFor(testConfig);
    }

    @Test
    void shouldHaveSameTFGMSourceConfigForNormalAndTrainEnabled() throws ConfigurationException, IOException {
        AppConfiguration normalConfig = loadConfigFromFile("local.yml");
        AppConfiguration gmConfig = loadConfigFromFile("gm.yml");

        // TODO Which other parameters should be the same?

        checkGTFSSourceConfig(normalConfig, gmConfig, Category.Closures.not(EnumSet.noneOf(Category.class)));

    }

    private void assertRailLiveData(OpenLdbConfig fromFile, OpenLdbConfig testConfig) {
        assertNotNull(fromFile);
        assertNotNull(testConfig);
    }

    @Test
    void shouldHaveKeyParamtersSameForGMRail() throws ConfigurationException, IOException {
        AppConfiguration appConfig = loadConfigFromFile("gm.yml");
        AppConfiguration testConfig = new RailAndTramGreaterManchesterConfig();

        validateCoreParameters(Collections.emptyList(), appConfig, testConfig);

        List<RemoteDataSourceConfig> configRemoteSources = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testRemoteSources = testConfig.getRemoteDataSourceConfig();

        assertEquals(appConfig.getNumberQueries(), testConfig.getNumberQueries(), "number of queries mismatch");
        assertEquals(appConfig.getQueryInterval(), testConfig.getQueryInterval(), "query interval mismatch");

        assertEquals(configRemoteSources.size(), testRemoteSources.size());
        assertEquals(4, testRemoteSources.size());

        assertRemoteSources(configRemoteSources, testRemoteSources, 0);
        assertRemoteSources(configRemoteSources, testRemoteSources, 1);
        assertRemoteSources(configRemoteSources, testRemoteSources, 2);
        assertRemoteSources(configRemoteSources, testRemoteSources, 3);

        RailConfig configRail = appConfig.getRailConfig();
        RailConfig testRail = testConfig.getRailConfig();

        assertEquals(configRail.getStations(), testRail.getStations());
        assertEquals(configRail.getTimetable(), testRail.getTimetable());
        assertEquals(configRail.getModes(), testRail.getModes());

        assertRailLiveData(appConfig.getOpenldbwsConfig(), testConfig.getOpenldbwsConfig());

        checkDataSourceConfig(configRail, testRail);

    }

    private void assertRemoteSources(List<RemoteDataSourceConfig> remoteSources, List<RemoteDataSourceConfig> testRemoteSources, int index) {
        final RemoteDataSourceConfig testRemoteSource = testRemoteSources.get(index);
        final RemoteDataSourceConfig remoteSource = remoteSources.get(index);
        assertEquals(remoteSource.getName(), testRemoteSource.getName());
        assertEquals(remoteSource.getDataCheckUrl(), testRemoteSource.getDataCheckUrl());
        //assertEquals(remoteSource.getDataUrl(), testRemoteSource.getDataUrl());
        assertTrue(remoteSource.getDataUrl().contains(testRemoteSource.getDataUrl()),
                remoteSource.getDataUrl() + " not matching " + testRemoteSource.getDataUrl());

        assertTrue(remoteSource.getDownloadFilename().contains(testRemoteSource.getDownloadFilename()),
                remoteSource.getDownloadFilename() + " did not contain " + testRemoteSource.getDownloadFilename());
        assertEquals(remoteSource.getDefaultExpiry(), testRemoteSource.getDefaultExpiry());
    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptance.yml");

        validateCoreParameters(Collections.emptySet(), appConfig, accTestConfig);

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = accTestConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(1, dataSourceConfig.size());

        assertRemoteSources(dataSourceConfig, testDataSourceConfig, 0);
    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTestsGM() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("gm.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptanceGM.yml");

        validateCoreParameters(Collections.singleton(Category.Modes), appConfig, accTestConfig);

        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "number of queries mismatch");
        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "query interval mismatch");

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");

        checkDataSourceConfig(appConfig.getRailConfig(), accTestConfig.getRailConfig());

        checkRailDataVersionFor(appConfig);
        checkRailDataVersionFor(accTestConfig);

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = accTestConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(4, dataSourceConfig.size());

        // rail tested above
        //assertRemoteSources(dataSourceConfig, testDataSourceConfig, 0);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, 1);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, 2);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, 3);

    }

    private void checkRailDataVersionFor(AppConfiguration appConfig) {
        String version = appConfig.getRailConfig().getVersion();
        RemoteDataSourceConfig dataSourceConfig = appConfig.getDataRemoteSourceConfig(DataSourceID.rail);
        String zip = String.format("ttis%s.zip", version);
        assertTrue(dataSourceConfig.getDataUrl().contains(zip),
                "Rail config and data source config mismatch? version:"+version+" Url: "+dataSourceConfig.getDataUrl());
    }

    private void validateCoreParameters(Collection<Category> excluded, AppConfiguration expected, AppConfiguration testConfig) {
        assertEquals(expected.getStaticAssetCacheTimeSeconds(), testConfig.getStaticAssetCacheTimeSeconds(), "StaticAssetCacheTimeSeconds");
        assertEquals(expected.getMaxJourneyDuration(), testConfig.getMaxJourneyDuration(), "MaxJourneyDuration");
        assertEquals(expected.getMaxWait(), testConfig.getMaxWait(), "MaxWait");
//        assertEquals(expected.getMaxInitialWait(), testConfig.getMaxInitialWait(), "MaxInitialWait");
        assertEquals(expected.getChangeAtInterchangeOnly(), testConfig.getChangeAtInterchangeOnly(), "ChangeAtInterchangeOnly");
        assertEquals(expected.getWalkingMPH(), testConfig.getWalkingMPH(), "WalkingMPH");
        assertEquals(expected.getNearestStopRangeKM(), testConfig.getNearestStopRangeKM(), "NearestStopRangeKM");
        assertEquals(expected.getNearestStopForWalkingRangeKM(), testConfig.getNearestStopForWalkingRangeKM(), "NearestStopForWalkingRangeKM");
        assertEquals(expected.getNumOfNearestStopsToOffer(), testConfig.getNumOfNearestStopsToOffer(), "NumOfNearestStopsToOffer");
        assertEquals(expected.getNumOfNearestStopsForWalking(), testConfig.getNumOfNearestStopsForWalking(), "NumOfNearestStopsForWalking");
        assertEquals(expected.getRecentStopsToShow(), testConfig.getRecentStopsToShow(), "RecentStopsToShow");
        assertEquals(expected.getMaxNumResults(), testConfig.getMaxNumResults(), "MaxNumResults");
        assertEquals(expected.getDistributionBucket(), testConfig.getDistributionBucket(), "distributionBucket");

        boolean hasNeighbourConfig = expected.hasNeighbourConfig();
        assertEquals(hasNeighbourConfig, testConfig.hasNeighbourConfig(), "has neighbour config");
        if (hasNeighbourConfig) {
            validateNeighbourConfig(expected, testConfig);
        }
        if (Category.Modes.not(excluded)) {
            assertEquals(expected.getTransportModes(), testConfig.getTransportModes(), "getTransportModes");
        }
        assertEquals(expected.getCalcTimeoutMillis(), testConfig.getCalcTimeoutMillis(), "CalcTimeoutMillis");
        assertEquals(expected.getPlanningEnabled(), testConfig.getPlanningEnabled(), "planningEnabled");

        if (Category.Bounds.not(excluded)) {
            assertEquals(expected.getBounds(), testConfig.getBounds(), "bounds");
        }

        checkDBConfig(expected, testConfig);

        checkGTFSSourceConfig(expected, testConfig, Category.Closures.not(excluded));

        checkRemoteDataSourceConfig(expected, testConfig);

    }

    private void validateNeighbourConfig(AppConfiguration appConfiguration, AppConfiguration testAppConfig) {
        NeighbourConfig expected = appConfiguration.getNeighbourConfig();
        NeighbourConfig testConfig = testAppConfig.getNeighbourConfig();

        assertEquals(expected.getMaxNeighbourConnections(), testConfig.getMaxNeighbourConnections(),
                "Max neighbour connections");
        assertEquals(expected.getDistanceToNeighboursKM(), testConfig.getDistanceToNeighboursKM(),
                "DistanceToNeighboursKM");
        List<StationIdPair> expectedAdditional = expected.getAdditional();
        assertEquals(expectedAdditional.size(), testConfig.getAdditional().size(), "additional neighbours");
        expectedAdditional.forEach(pair ->
                assertTrue(testConfig.getAdditional().contains(pair),
                        pair.toString() + " is missing from " + testConfig.getAdditional()));
    }

    private void checkRemoteDataSourceConfig(AppConfiguration expected, AppConfiguration testConfig) {
        List<RemoteDataSourceConfig> expectedRemoteDataSourceConfig = expected.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> foundRemoteDataSourceConfig = testConfig.getRemoteDataSourceConfig();

        assertFalse(expectedRemoteDataSourceConfig.isEmpty());
        assertEquals(expectedRemoteDataSourceConfig.size(), foundRemoteDataSourceConfig.size(), "RemoteDataSourceConfig");
        for (int i = 0; i < expectedRemoteDataSourceConfig.size(); i++) {
            RemoteDataSourceConfig expectedRemote = expectedRemoteDataSourceConfig.get(0);
            RemoteDataSourceConfig foundRemote = foundRemoteDataSourceConfig.get(0);
            //assertEquals(expectedRemote.getDataUrl(), foundRemote.getDataUrl());
            assertEquals(expectedRemote.getDataCheckUrl(), foundRemote.getDataCheckUrl(), "DataCheckUrl");
        }
    }

    private void checkGTFSSourceConfig(AppConfiguration expected, AppConfiguration testConfig, boolean checkClosures) {
        List<GTFSSourceConfig> expectedgtfsDataSource = expected.getGTFSDataSource();
        List<GTFSSourceConfig> foundgtfsDataSource = testConfig.getGTFSDataSource();
        assertEquals(expectedgtfsDataSource.size(), foundgtfsDataSource.size());
        //assume same order
        for (int i = 0; i < expectedgtfsDataSource.size(); i++) {
            GTFSSourceConfig expectedDataSource = expectedgtfsDataSource.get(i);
            GTFSSourceConfig dataSourceConfig = foundgtfsDataSource.get(i);

            assertEquals(expectedDataSource.getNoServices(), dataSourceConfig.getNoServices() , "NoServices");
            assertEquals(expectedDataSource.getTransportGTFSModes(), dataSourceConfig.getTransportGTFSModes(), "TransportGTFSModes");
            assertEquals(expectedDataSource.getAdditionalInterchanges(), dataSourceConfig.getAdditionalInterchanges(), "AdditionalInterchanges");
            if (checkClosures) {
                assertEquals(expectedDataSource.getStationClosures(), dataSourceConfig.getStationClosures(), "station closures");
            }
            assertEquals(expectedDataSource.getAddWalksForClosed(), dataSourceConfig.getAddWalksForClosed(), "AddWalksForClosed");

            checkDataSourceConfig(expectedDataSource, dataSourceConfig);
        }
    }

    private void checkDataSourceConfig(TransportDataSourceConfig expected, TransportDataSourceConfig testConfig) {
        assertEquals(expected.getMaxInitialWait(), testConfig.getMaxInitialWait());
        assertEquals(expected.getDataSourceId(), testConfig.getDataSourceId());
        assertEquals(expected.getOnlyMarkedInterchanges(), testConfig.getOnlyMarkedInterchanges());
    }

    private void checkDBConfig(AppConfiguration expected, AppConfiguration testConfig) {
        GraphDBConfig expectedGraphDBConfig = expected.getGraphDBConfig();
        GraphDBConfig testGraphDBConfig = testConfig.getGraphDBConfig();
        assertEquals(expectedGraphDBConfig.getNeo4jPagecacheMemory(), testGraphDBConfig.getNeo4jPagecacheMemory(),
                "neo4jPagecacheMemory");

        TfgmTramLiveDataConfig expectedLiveDataConfig = expected.getLiveDataConfig();

        if (expectedLiveDataConfig!=null) {
            TfgmTramLiveDataConfig liveDataConfig = testConfig.getLiveDataConfig();
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutMessages(), liveDataConfig.getMaxNumberStationsWithoutMessages());
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutData(), liveDataConfig.getMaxNumberStationsWithoutData());
        } else {
            assertNull(testConfig.getLiveDataConfig());
        }
    }

    private AppConfiguration loadConfigFromFile(String configFilename) throws IOException, ConfigurationException {
        Path mainConfig = Paths.get("config", configFilename).toAbsolutePath();
        return loadConfigFromFile(mainConfig);
    }

    private AppConfiguration loadConfigFromFile(Path fullPathToConfig) throws IOException, ConfigurationException {
        YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();

        return factory.build(fullPathToConfig.toFile());
    }

    @NotNull
    private YamlConfigurationFactory<AppConfiguration> getValidatingFactory() {
        Class<AppConfiguration> klass = AppConfiguration.class;
        Validator validator = null;
        ObjectMapper objectMapper = Jackson.newObjectMapper();

        String properyPrefix = "dw";
        return new YamlConfigurationFactory<>(klass, validator, objectMapper, properyPrefix);
    }

    @NotNull
    private Set<Path> getConfigFiles() throws IOException {
        Path configDir = Paths.get("config").toAbsolutePath();
        return Files.list(configDir).
                filter(Files::isRegularFile).
                filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml")).
                collect(Collectors.toSet());
    }

}
