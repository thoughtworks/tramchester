package com.tramchester.graph;

import com.tramchester.domain.TramServiceDate;
import org.neo4j.graphdb.Node;

public interface NodeOperations {

    boolean[] getDays(Node node);

    TramServiceDate getServiceStartDate(Node node);

    TramServiceDate getServiceEndDate(Node node);

    int getHour(Node node);

    boolean isService(Node node);

    boolean isHour(Node node);
}
