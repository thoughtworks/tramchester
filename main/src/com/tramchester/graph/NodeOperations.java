package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

public interface NodeOperations {

    boolean isService(Node node);

    boolean isHour(Node node);

    boolean isTime(Node node);

    int getHour(Node node);

    LocalTime getTime(Node node);

    String getServiceId(Node node);

    String getServiceId(Relationship inbound);

    TramTime getServiceEarliest(Node node);

    TramTime getServiceLatest(Node node);

    int getCost(Relationship relationship);

}
