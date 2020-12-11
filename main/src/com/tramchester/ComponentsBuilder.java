package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.IncludeAllFilter;
import com.tramchester.repository.TransportDataProvider;

public class ComponentsBuilder<C extends TransportDataProvider> {
    private GraphFilter graphFilter = new IncludeAllFilter();
    private Class<C> overrideType;

    public ComponentsBuilder<C> setGraphFilter(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
        return this;
    }

    public ComponentContainer create(TramchesterConfig config) {
        if (overrideType==null) {
            return new GuiceContainerDependencies(graphFilter, config);
        } else {
            return new GuiceContainerDependencies(graphFilter, config, overrideType);
        }
    }

    public ComponentsBuilder<C> overrideProvider(Class<C> providerClass) {
        this.overrideType = providerClass;
        return this;
    }
}