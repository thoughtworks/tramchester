package com.tramchester.unit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.AppConfiguration;
import com.tramchester.integration.IntegrationTramTestConfig;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.Test;

import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigMismatchTest {

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
        assertEquals(expected.getMaxNumberStationsWithoutMessages(), testConfig.getMaxNumberStationsWithoutMessages());
        assertEquals(expected.getMaxNumberStationsWithoutData(), testConfig.getMaxNumberStationsWithoutData());
    }

    private AppConfiguration loadConfigFromFile(String configFilename) throws IOException, ConfigurationException {
        Path mainConfig = Paths.get("config", configFilename).toAbsolutePath();

        Class<AppConfiguration> klass = AppConfiguration.class;
        Validator validator = null;
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        String properyPrefix = "dw";
        YamlConfigurationFactory<AppConfiguration> factory = new YamlConfigurationFactory<>(klass, validator, objectMapper, properyPrefix);

        return factory.build(mainConfig.toFile());
    }

}
