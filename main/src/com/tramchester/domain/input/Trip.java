package com.tramchester.domain.input;

import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

public interface Trip extends HasId<Trip>, HasTransportMode, GraphProperty {

    IdFor<Trip> getId();

    StopCalls getStopCalls();

    Service getService();

    String getHeadsign();

    Route getRoute();

    TramTime earliestDepartTime();

    TramTime latestDepartTime();

    TransportMode getTransportMode();

    int getSeqNumOfFirstStop();

    int getSeqNumOfLastStop();

    GraphPropertyKey getProp();

    boolean isFiltered();
}
