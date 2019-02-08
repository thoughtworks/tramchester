package com.tramchester.graph;

import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public interface NodeOperations {

    boolean isService(Node node);

    boolean isHour(Node node);

    boolean isMinute(Node node);

    int getHour(Node endNode);

    LocalTime getTime(Node node);

    String getServiceId(Node node);
}
