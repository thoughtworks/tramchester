package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class GraphDBAppConfig extends Configuration implements GraphDBConfig {

    private final String graphName;
    private final String neo4jPagecacheMemory;

    public GraphDBAppConfig(@JsonProperty(value = "graphName", required = true) String graphName,
                            @JsonProperty(value = "neo4jPagecacheMemory", required = true) String neo4jPagecacheMemory) {
        this.graphName = graphName;
        this.neo4jPagecacheMemory = neo4jPagecacheMemory;
    }

    // name of the graph DB to use
    @Override
    public String getGraphName() {
        return graphName;
    }

    // page cache memory for neo4j
    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
    @Override
    public String getNeo4jPagecacheMemory() {
        return neo4jPagecacheMemory;
    }

    @Override
    public String toString() {
        return "GraphDBAppConfig{" +
                "graphName='" + graphName + '\'' +
                ", neo4jPagecacheMemory='" + neo4jPagecacheMemory + '\'' +
                '}';
    }
}
