package com.tramchester.unit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.*;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.validation.Validator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMismatchTest {

    @Test
    void shouldBeAbleToLoadAllConfigWithoutExceptions() throws IOException, ConfigurationException {
        // Note: this does not catch all the same validation cases as app start up
        @NotNull YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();
        Path configDir = Paths.get("config").toAbsolutePath();
        Set<Path> configFiles = Files.list(configDir).
                filter(Files::isRegularFile).
                filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml")).
                collect(Collectors.toSet());

        for (Path config : configFiles) {
            factory.build(config.toFile());
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

        validateCoreParameters(false, appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForBusIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("buses.yml");
        IntegrationBusTestConfig testConfig = new IntegrationBusTestConfig();

        validateCoreParameters(true, appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForRailntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("rail.yml");
        IntegrationRailTestConfig testConfig = new IntegrationRailTestConfig();

        validateCoreParameters(true, appConfig, testConfig);

        List<RemoteDataSourceConfig> remoteSources = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testRemoteSources = testConfig.getRemoteDataSourceConfig();
        assertEquals(remoteSources.size(), testRemoteSources.size());
        assertEquals(1, testRemoteSources.size());

        assertEquals(remoteSources.get(0).getDataCheckUrl(), testRemoteSources.get(0).getDataCheckUrl());
        assertEquals(remoteSources.get(0).getDataUrl(), testRemoteSources.get(0).getDataUrl());
        assertEquals(remoteSources.get(0).getDownloadFilename(), testRemoteSources.get(0).getDownloadFilename());

        RailConfig rail = appConfig.getRailConfig();
        RailConfig testRail = appConfig.getRailConfig();

        assertEquals(rail.getStations(), testRail.getStations());
        assertEquals(rail.getTimetable(), testRail.getTimetable());
        assertEquals(rail.getModes(), testRail.getModes());

    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptance.yml");

        validateCoreParameters(true, appConfig, accTestConfig);

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");
    }

    private void validateCoreParameters(boolean checkClosures, AppConfiguration expected, AppConfiguration testConfig) {
        assertEquals(expected.getStaticAssetCacheTimeSeconds(), testConfig.getStaticAssetCacheTimeSeconds(), "StaticAssetCacheTimeSeconds");
        assertEquals(expected.getMaxJourneyDuration(), testConfig.getMaxJourneyDuration(), "MaxJourneyDuration");
        assertEquals(expected.getMaxWait(), testConfig.getMaxWait(), "MaxWait");
        assertEquals(expected.getMaxInitialWait(), testConfig.getMaxInitialWait(), "MaxInitialWait");
        assertEquals(expected.getChangeAtInterchangeOnly(), testConfig.getChangeAtInterchangeOnly(), "ChangeAtInterchangeOnly");
        assertEquals(expected.getWalkingMPH(), testConfig.getWalkingMPH(), "WalkingMPH");
        assertEquals(expected.getNearestStopRangeKM(), testConfig.getNearestStopRangeKM(), "NearestStopRangeKM");
        assertEquals(expected.getNearestStopForWalkingRangeKM(), testConfig.getNearestStopForWalkingRangeKM(), "NearestStopForWalkingRangeKM");
        assertEquals(expected.getNumOfNearestStopsToOffer(), testConfig.getNumOfNearestStopsToOffer(), "NumOfNearestStopsToOffer");
        assertEquals(expected.getNumOfNearestStopsForWalking(), testConfig.getNumOfNearestStopsForWalking(), "NumOfNearestStopsForWalking");
        assertEquals(expected.getRecentStopsToShow(), testConfig.getRecentStopsToShow(), "RecentStopsToShow");
        assertEquals(expected.getMaxNumResults(), testConfig.getMaxNumResults(), "MaxNumResults");
        assertEquals(expected.getCreateNeighbours(), testConfig.getCreateNeighbours(), "CreateNeighbours");
        assertEquals(expected.getMaxNeighbourConnections(), testConfig.getMaxNeighbourConnections(), "Max neighbour connections");
        assertEquals(expected.getDistanceToNeighboursKM(), testConfig.getDistanceToNeighboursKM(), "DistanceToNeighboursKM");
        assertEquals(expected.getTransportModes(), testConfig.getTransportModes(), "getTransportModes");
        assertEquals(expected.getCalcTimeoutMillis(), testConfig.getCalcTimeoutMillis(), "CalcTimeoutMillis");
        assertEquals(expected.getPlanningEnabled(), testConfig.getPlanningEnabled(), "planningEnabled");

        assertEquals(expected.getBounds(), testConfig.getBounds(), "bounds");

        checkDBConfig(expected, testConfig);

        checkGTFSSourceConfig(expected, testConfig, checkClosures);

        checkRemoteDataSourceConfig(expected, testConfig);

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
        }
    }

    private void checkDBConfig(AppConfiguration expected, AppConfiguration testConfig) {
        GraphDBConfig expectedGraphDBConfig = expected.getGraphDBConfig();
        GraphDBConfig testGraphDBConfig = testConfig.getGraphDBConfig();
        assertEquals(expectedGraphDBConfig.getNeo4jPagecacheMemory(), testGraphDBConfig.getNeo4jPagecacheMemory());

        LiveDataConfig expectedLiveDataConfig = expected.getLiveDataConfig();

        if (expectedLiveDataConfig!=null) {
            LiveDataConfig liveDataConfig = testConfig.getLiveDataConfig();
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutMessages(), liveDataConfig.getMaxNumberStationsWithoutMessages());
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutData(), liveDataConfig.getMaxNumberStationsWithoutData());
        } else {
            assertNull(testConfig.getLiveDataConfig());
        }
    }

    private AppConfiguration loadConfigFromFile(String configFilename) throws IOException, ConfigurationException {
        Path mainConfig = Paths.get("config", configFilename).toAbsolutePath();

        YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();

        return factory.build(mainConfig.toFile());
    }

    @NotNull
    private YamlConfigurationFactory<AppConfiguration> getValidatingFactory() {
        Class<AppConfiguration> klass = AppConfiguration.class;
        Validator validator = null;
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        String properyPrefix = "dw";
        return new YamlConfigurationFactory<>(klass, validator, objectMapper, properyPrefix);
    }

}
