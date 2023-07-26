package com.tramchester.deployment.cli;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.metrics.CacheMetrics;
import io.dropwizard.configuration.*;
import io.dropwizard.jackson.Jackson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseCLI {
    @NotNull
    protected GuiceContainerDependencies bootstrap(Path configFile, String name) throws IOException, ConfigurationException {
        TramchesterConfig configuration = loadConfigFromFile(configFile);
        configuration.getLoggingFactory().configure(new MetricRegistry(), name);

        GuiceContainerDependencies container = new ComponentsBuilder().create(configuration, new NoOpCacheMetrics());
        container.initialise();
        return container;
    }

    protected void run(Path configFile, Logger logger, String name) throws ConfigurationException, IOException {
        GuiceContainerDependencies container = bootstrap(configFile, name);
        final TramchesterConfig config = container.get(TramchesterConfig.class);
        run(logger, container, config);
        container.close();
    }

    public abstract  void run(Logger logger,  GuiceContainerDependencies dependencies, TramchesterConfig config);

    private YamlConfigurationFactory<AppConfiguration> getValidatingFactory() {
        Class<AppConfiguration> klass = AppConfiguration.class;
        Validator validator = null;
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        String properyPrefix = "dw"; // dropwizard
        return new YamlConfigurationFactory<>(klass, validator, objectMapper, properyPrefix);
    }

    private AppConfiguration loadConfigFromFile(Path config) throws IOException, ConfigurationException {

        FileConfigurationSourceProvider originalProvider = new FileConfigurationSourceProvider();
        SubstitutingSourceProvider provider = new SubstitutingSourceProvider(
                originalProvider,
                new EnvironmentVariableSubstitutor(false));

        YamlConfigurationFactory<AppConfiguration> factory = getValidatingFactory();

        return factory.build(provider, config.toString());
    }

    private static class NoOpCacheMetrics implements CacheMetrics.RegistersCacheMetrics {
        @Override
        public <T> void register(String metricName, Gauge<T> Gauge) {
            // noop
        }
    }
}
