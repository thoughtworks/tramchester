package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.IncludeAllFilter;

public class ComponentsBuilder {
    private GraphFilter graphFilter = new IncludeAllFilter();

    public ComponentsBuilder setGraphFilter(GraphFilter graphFilter) {
        this.graphFilter = graphFilter;
        return this;
    }

    public ComponentContainer create(TramchesterConfig config) {
        //return new GuiceContainerDependencies(graphFilter, config);
        return new PicoContainerDependencies(graphFilter, config);
    }
}