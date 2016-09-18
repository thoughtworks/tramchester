package com.tramchester;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.TransportDataFromFiles;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class AcceptanceTestRun extends DropwizardAppRule<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    private String serverUrl;

    public AcceptanceTestRun(Class<? extends Application<AppConfiguration>> applicationClass, String configPath,
                             ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
        serverUrl = System.getenv("SERVER_URL");
    }

    @Override
    protected void before() {
        if (localRun()) {
            super.before();
        }
    }

    private boolean localRun() {
        return serverUrl==null;
    }

    @Override
    protected void after() {
        if (localRun()) {
            super.after();
        }
    }

    public String getUrl() {
        if (localRun()) {
            return "http://localhost:"+getLocalPort();
        }
        return serverUrl;
    }

    public FeedInfo feedinfo() {
        App app = super.getApplication();
        TransportDataFromFiles data = app.getDependencies().get(TransportDataFromFiles.class);
        return data.getFeedInfo();
    }
}
