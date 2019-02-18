package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public interface NodeOperations {

    boolean isService(Node node);

    boolean isHour(Node node);

    boolean isTime(Node node);

    int getHour(Node endNode);

    LocalTime getTime(Node node);

    String getServiceId(Node node);

    TramTime getServiceEarliest(Node node);

    TramTime getServiceLatest(Node node);
}
