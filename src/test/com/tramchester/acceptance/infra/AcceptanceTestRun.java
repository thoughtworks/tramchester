package com.tramchester.acceptance.infra;

import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.util.Optional;

import static java.lang.String.format;

public class AcceptanceTestRun extends DropwizardAppRule<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    private Optional<String> serverURLFromEnv;
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
    protected void before() {
        if (localRun()) {
            super.before();
        }
    }

    private boolean localRun() {
        return !serverURLFromEnv.isPresent();
    }

    @Override
    protected void after() {
        if (localRun()) {
            super.after();
        }
    }

    public String getUrl() {
        if (serverURLFromEnv.isPresent()) {
            return serverURLFromEnv.get();
        } else {
            return format("http://%s:%s", localRunHost, getLocalPort());
        }
    }

}
