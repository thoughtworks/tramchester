package com.tramchester.acceptance.infra;

import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

import java.util.Optional;

import static java.lang.String.format;

public class AcceptanceAppExtenstion extends DropwizardAppExtension<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> serverURLFromEnv;
    private final String localRunHost;

    public AcceptanceAppExtenstion(Class<? extends Application<AppConfiguration>> applicationClass, String configPath,
                                   ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
        serverURLFromEnv = Optional.ofNullable(System.getenv("SERVER_URL"));
        localRunHost = createLocalURL();
    }

    private String createLocalURL() {
        return "localhost";
    }

    @Override
    public void before() throws Exception {
        if (localRun()) {
            // start local server
            super.before();
        }
    }

    private boolean localRun() {
        return serverURLFromEnv.isEmpty();
    }

    @Override
    public void after() {
        if (localRun()) {
            // stop local server
            super.after();
        }
    }

    public String getUrl() {
        return serverURLFromEnv.orElseGet(() -> format("http://%s:%s", localRunHost, getLocalPort()));
    }

}
