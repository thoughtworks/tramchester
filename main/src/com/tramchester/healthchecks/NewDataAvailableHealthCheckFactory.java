package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.ServiceTimeLimits;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@LazySingleton
public class NewDataAvailableHealthCheckFactory implements HealthCheckFactory {

    private final TramchesterConfig config;
    private final URLDownloadAndModTime urlDownloader;
    private final FetchFileModTime fileModTime;
    private final List<TramchesterHealthCheck> healthCheckList;
    private final ServiceTimeLimits serviceTimeLimits;

    @Inject
    public NewDataAvailableHealthCheckFactory(TramchesterConfig config, URLDownloadAndModTime urlDownloader,
                                              FetchFileModTime fileModTime, ServiceTimeLimits serviceTimeLimits) {
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fileModTime = fileModTime;
        this.serviceTimeLimits = serviceTimeLimits;
        healthCheckList = new ArrayList<>();
    }

    public Collection<TramchesterHealthCheck> getHealthChecks() {
        return healthCheckList;
    }

    @PreDestroy
    public void dispose() {
        healthCheckList.clear();
    }

    @PostConstruct
    public void start() {
        config.getRemoteDataSourceConfig().forEach(config ->
                healthCheckList.add(new NewDataAvailableHealthCheck(config, urlDownloader, fileModTime, serviceTimeLimits)));
    }

}
