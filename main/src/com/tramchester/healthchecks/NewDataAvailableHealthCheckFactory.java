package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewDataAvailableHealthCheckFactory implements Startable, Disposable {

    private final TramchesterConfig config;
    private final URLDownloadAndModTime urlDownloader;
    private final FetchFileModTime fileModTime;
    private final List<NewDataAvailableHealthCheck> healthCheckList;

    public NewDataAvailableHealthCheckFactory(TramchesterConfig config, URLDownloadAndModTime urlDownloader, FetchFileModTime fileModTime) {
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fileModTime = fileModTime;
        healthCheckList = new ArrayList<>();
    }

    public Collection<NewDataAvailableHealthCheck> getHealthChecks() {
        return healthCheckList;
    }

    @Override
    public void dispose() {
        healthCheckList.clear();
    }

    @Override
    public void start() {
        config.getDataSourceConfig().forEach(config -> {
            healthCheckList.add(new NewDataAvailableHealthCheck(config, urlDownloader, fileModTime));
        });
    }

    @Override
    public void stop() {
        // no-op
    }
}
