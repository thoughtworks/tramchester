package com.tramchester.infra;

import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.util.Optional;

public class AcceptanceTestRun extends DropwizardAppRule<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    private Optional<String> serverUrl;

    public AcceptanceTestRun(Class<? extends Application<AppConfiguration>> applicationClass, String configPath,
                             ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
        serverUrl = Optional.ofNullable(System.getenv("SERVER_URL"));
    }

    @Override
    protected void before() {
        if (localRun()) {
            super.before();
        }
    }

    private boolean localRun() {
        return !serverUrl.isPresent();
    }

    @Override
    protected void after() {
        if (localRun()) {
            super.after();
        }
    }

    public String getUrl() {
        return serverUrl.orElse("http://localhost:"+getLocalPort());
    }

}
