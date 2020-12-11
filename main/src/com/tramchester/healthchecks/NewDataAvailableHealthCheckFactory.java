package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class NewDataAvailableHealthCheckFactory implements Startable, Disposable, HealthCheckFactory {

    private final TramchesterConfig config;
    private final URLDownloadAndModTime urlDownloader;
    private final FetchFileModTime fileModTime;
    private final List<TramchesterHealthCheck> healthCheckList;

    @Inject
    public NewDataAvailableHealthCheckFactory(TramchesterConfig config, URLDownloadAndModTime urlDownloader, FetchFileModTime fileModTime) {
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fileModTime = fileModTime;
        healthCheckList = new ArrayList<>();
    }

    public Collection<TramchesterHealthCheck> getHealthChecks() {
        return healthCheckList;
    }

    @PreDestroy
    @Override
    public void dispose() {
        healthCheckList.clear();
    }

    @PostConstruct
    @Override
    public void start() {
        config.getDataSourceConfig().forEach(config ->
                healthCheckList.add(new NewDataAvailableHealthCheck(config, urlDownloader, fileModTime)));
    }

    @Override
    public void stop() {
        // no-op
    }
}
