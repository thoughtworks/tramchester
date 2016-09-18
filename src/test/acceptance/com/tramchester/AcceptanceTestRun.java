package com.tramchester;

import com.tramchester.config.AppConfiguration;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.TransportDataFromFiles;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AcceptanceTestRun extends DropwizardAppRule<AppConfiguration> {

    // if SERVER_URL env var not set then run against localhost
    private String serverUrl;
    private FeedInfo feedinfo;

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
        if (feedinfo==null) {
            // can't get via the app as when we run in snap the app is not running locally
            Path path = Paths.get("data/tram");
            TransportDataReader reader = new TransportDataReader(path);
            TransportDataImporter importer = new TransportDataImporter(reader);
            TransportDataFromFiles data = importer.load();
            feedinfo = data.getFeedInfo();
        }
        return feedinfo;

    }
}
