package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.filters.ActiveGraphFilter;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.IncludeAllFilter;

public class GraphFilterModule extends AbstractModule {

    private final ComponentsBuilder.SetupGraphFilter setupGraphFilter;

    public GraphFilterModule(ComponentsBuilder.SetupGraphFilter setupGraphFilter) {
        this.setupGraphFilter = setupGraphFilter;
    }

    @SuppressWarnings("unused")
    @LazySingleton
    @Provides
    GraphFilter providesConfiguredGraphFilter() {
        if (setupGraphFilter==null) {
            return new IncludeAllFilter();
        }

        ActiveGraphFilter activeGraphFilter = new ActiveGraphFilter();
        setupGraphFilter.configure(activeGraphFilter);
        return activeGraphFilter;
    }
}
