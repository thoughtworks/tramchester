package com.tramchester.unit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.*;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    void shouldHaveKeyParametersSameForTramIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(true);

        validateCoreParameters(appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForBusIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("buses.yml");
        IntegrationBusTestConfig testConfig = new IntegrationBusTestConfig();

        validateCoreParameters(appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForTrainIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("trains.yml");
        IntegrationTrainTestConfig testConfig = new IntegrationTrainTestConfig();

        validateCoreParameters(appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptance.yml");

        validateCoreParameters(appConfig, accTestConfig);

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");
    }

    private void validateCoreParameters(AppConfiguration expected, AppConfiguration testConfig) {
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
        assertEquals(expected.getDistanceToNeighboursKM(), testConfig.getDistanceToNeighboursKM(), "DistanceToNeighboursKM");
        assertEquals(expected.getTransportModes(), testConfig.getTransportModes(), "getTransportModes");
        assertEquals(expected.getCalcTimeoutMillis(), testConfig.getCalcTimeoutMillis(), "CalcTimeoutMillis");

        assertEquals(expected.getBounds(), testConfig.getBounds(), "bounds");

        checkDBConfig(expected, testConfig);

        checkGTFSSourceConfig(expected, testConfig);

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

    private void checkGTFSSourceConfig(AppConfiguration expected, AppConfiguration testConfig) {
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
            //assertEquals(expectedDataSource.getStationClosures(), dataSourceConfig.getStationClosures(), "station closures");
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
