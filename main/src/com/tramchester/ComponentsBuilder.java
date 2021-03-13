package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.TransportDataFactory;
import com.tramchester.repository.TransportDataFromFiles;

public class ComponentsBuilder<C extends TransportDataFactory> {
    private GraphFilter graphFilter = new IncludeAllFilter();
    private Class<C> transportDataFactoryType;

    public ComponentsBuilder() {
        // TODO
        this.transportDataFactoryType = (Class<C>) TransportDataFromFiles.class;
    }

    public ComponentsBuilder<C> setGraphFilter(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
        return this;
    }

    public GuiceContainerDependencies<C> create(TramchesterConfig config, CacheMetrics.RegistersCacheMetrics registerCacheMetrics) {
        return new GuiceContainerDependencies<>(graphFilter, config, registerCacheMetrics, transportDataFactoryType);
    }

    public ComponentsBuilder<C> overrideProvider(Class<C> providerClass) {
        this.transportDataFactoryType = providerClass;
        return this;
    }

}