package com.tramchester.acceptance.infra;

import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

import java.util.Optional;

import static java.lang.String.format;

public class AcceptanceTestRun extends DropwizardAppExtension<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    private final Optional<String> serverURLFromEnv;
    private final String localRunHost;

    public AcceptanceTestRun(Class<? extends Application<AppConfiguration>> applicationClass, String configPath,
                             ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
        serverURLFromEnv = Optional.ofNullable(System.getenv("SERVER_URL"));
        localRunHost = createLocalURL();
    }

    private String createLocalURL() {
        Optional<String> android = Optional.ofNullable(System.getProperty("android"));

        if (android.isPresent()) {
            if (android.get().equals("true")) {
                return "10.0.2.2";
            }
        }
        return "localhost";
    }

    @Override
    public void before() throws Exception {
        if (localRun()) {
            super.before();
        }
    }

    private boolean localRun() {
        return serverURLFromEnv.isEmpty();
    }

    @Override
    public void after() {
        if (localRun()) {
            super.after();
        }
    }

    public String getUrl() {
        return serverURLFromEnv.orElseGet(() -> format("http://%s:%s", localRunHost, getLocalPort()));
    }

}
