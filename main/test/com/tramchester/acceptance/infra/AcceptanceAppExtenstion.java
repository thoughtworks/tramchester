package com.tramchester.acceptance.infra;

import com.tramchester.config.AppConfiguration;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.core.Application;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

import java.util.Optional;

import static java.lang.String.format;

public class AcceptanceAppExtenstion extends DropwizardAppExtension<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> serverURLFromEnv;
    private final String localRunHost;

    public AcceptanceAppExtenstion(Class<? extends Application<AppConfiguration>> applicationClass, String configPath) {
        super(applicationClass, configPath);
        serverURLFromEnv = Optional.ofNullable(System.getenv(TestEnv.SERVER_URL_ENV_VAR));
        localRunHost = createLocalURL();
    }

    @Override
    public void before() throws Exception {
        if (localRun()) {
            // start local server
            super.before();
        }
    }

    @Override
    public void after() {
        if (localRun()) {
            // stop local server
            super.after();
        }
    }

    private String createLocalURL() {
        return "localhost";
    }

    private boolean localRun() {
        return serverURLFromEnv.isEmpty();
    }

    public String getUrl() {
        return serverURLFromEnv.orElseGet(() -> format("http://%s:%s", localRunHost, getLocalPort()));
    }

}
