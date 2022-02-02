package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.filters.ActiveGraphFilter;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.graph.filters.IncludeAllFilter;
import com.tramchester.repository.TransportData;

public class GraphFilterModule extends AbstractModule {

    private final ComponentsBuilder.SetupGraphFilter overideDefaultIncludeAllFilter;

    public GraphFilterModule(ComponentsBuilder.SetupGraphFilter overideDefaultIncludeAllFilter) {
        this.overideDefaultIncludeAllFilter = overideDefaultIncludeAllFilter;
    }

    @LazySingleton
    @Provides
    GraphFilterActive providesGraphFilterPresent() {
        return new GraphFilterActive(overideDefaultIncludeAllFilter !=null);
    }

    @LazySingleton
    @Provides
    GraphFilter providesConfiguredGraphFilter(TransportData transportData) {
        if (overideDefaultIncludeAllFilter == null) {
            return new IncludeAllFilter();
        }

        ActiveGraphFilter activeGraphFilter = new ActiveGraphFilter();
        overideDefaultIncludeAllFilter.configure(activeGraphFilter, transportData);
        return activeGraphFilter;
    }
}
