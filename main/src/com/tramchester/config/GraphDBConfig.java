package com.tramchester.config;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;

@JsonDeserialize(as=GraphDBAppConfig.class)
public interface GraphDBConfig {

    Path getDbPath();

    // page cache memory for neo4j
    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
    String getNeo4jPagecacheMemory();

    String getInitialHeapSize();
    String getMaxHeapSize();
}
