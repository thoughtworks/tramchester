package com.tramchester.config;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as=GraphDBAppConfig.class)
public interface GraphDBConfig {
    // name of the graph DB to use
    String getGraphName();

    // page cache memory for neo4j
    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
    String getNeo4jPagecacheMemory();
}
