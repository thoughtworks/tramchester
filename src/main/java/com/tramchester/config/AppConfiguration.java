package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration extends Configuration {
    @JsonProperty("rebuildGraph")
    private boolean rebuildGraph;

    public boolean isRebuildGraph() {
        return rebuildGraph;
    }
}
