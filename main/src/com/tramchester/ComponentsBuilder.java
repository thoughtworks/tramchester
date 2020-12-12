package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.repository.TransportDataProvider;

public class ComponentsBuilder<C extends TransportDataProvider> {
    private GraphFilter graphFilter = new IncludeAllFilter();
    private Class<C> overrideTransportData;

    public ComponentsBuilder<C> setGraphFilter(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
        return this;
    }

    public ComponentContainer create(TramchesterConfig config, CacheMetrics.RegistersMetrics registerMetrics) {
        if (overrideTransportData ==null) {
            return new GuiceContainerDependencies(graphFilter, config, registerMetrics);
        } else {
            return new GuiceContainerDependencies(graphFilter, config, overrideTransportData, registerMetrics);
        }
    }

    public ComponentsBuilder<C> overrideProvider(Class<C> providerClass) {
        this.overrideTransportData = providerClass;
        return this;
    }

}