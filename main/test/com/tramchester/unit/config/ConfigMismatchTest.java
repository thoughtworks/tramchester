package com.tramchester.unit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.integration.IntegrationTramTestConfig;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigMismatchTest {

    @Test
    void shouldBeAbleToLoadAllConfigWithoutExceptions() throws IOException, ConfigurationException {
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
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();

        validateCoreParameters(appConfig, testConfig);

//        assertEquals(appConfig.getQueryInterval(), testConfig.getQueryInterval());
//        assertEquals(appConfig.getNumberQueries(), testConfig.getNumberQueries());
    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        AppConfiguration testConfig = loadConfigFromFile("localAcceptance.yml");

        validateCoreParameters(appConfig, testConfig);

        assertEquals(appConfig.getQueryInterval(), testConfig.getQueryInterval());
        assertEquals(appConfig.getNumberQueries(), testConfig.getNumberQueries());

    }

    private void validateCoreParameters(AppConfiguration expected, AppConfiguration testConfig) {
        assertEquals(expected.getMaxJourneyDuration(), testConfig.getMaxJourneyDuration());
        assertEquals(expected.getMaxWait(), testConfig.getMaxWait());
        assertEquals(expected.getChangeAtInterchangeOnly(), testConfig.getChangeAtInterchangeOnly());
        assertEquals(expected.getWalkingMPH(), testConfig.getWalkingMPH());
        assertEquals(expected.getNearestStopRangeKM(), testConfig.getNearestStopRangeKM());
        assertEquals(expected.getNearestStopForWalkingRangeKM(), testConfig.getNearestStopForWalkingRangeKM());
        assertEquals(expected.getNumOfNearestStopsToOffer(), testConfig.getNumOfNearestStopsToOffer());
        assertEquals(expected.getNumOfNearestStopsForWalking(), testConfig.getNumOfNearestStopsForWalking());
        assertEquals(expected.getRecentStopsToShow(), testConfig.getRecentStopsToShow());
        assertEquals(expected.getMaxNumResults(), testConfig.getMaxNumResults());
        assertEquals(expected.getCreateNeighbours(), testConfig.getCreateNeighbours());
        assertEquals(expected.getDistanceToNeighboursKM(), testConfig.getDistanceToNeighboursKM());
        assertEquals(expected.getLoadPostcodes(), testConfig.getLoadPostcodes());
        assertEquals(expected.getTransportModes(), testConfig.getTransportModes());

        LiveDataConfig expectedLiveDataConfig = expected.getLiveDataConfig();
        LiveDataConfig liveDataConfig = testConfig.getLiveDataConfig();
        assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutMessages(), liveDataConfig.getMaxNumberStationsWithoutMessages());
        assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutData(), liveDataConfig.getMaxNumberStationsWithoutData());
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
