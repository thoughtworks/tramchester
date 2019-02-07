package com.tramchester.graph;

import com.tramchester.domain.TramServiceDate;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public interface NodeOperations {

    boolean[] getDays(Node node);

    TramServiceDate getServiceStartDate(Node node);

    TramServiceDate getServiceEndDate(Node node);

    boolean isService(Node node);

    boolean isHour(Node node);

    boolean isMinute(Node node);

    int getHour(Node endNode);

    LocalTime getTime(Node node);

}
