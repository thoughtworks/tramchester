package com.tramchester.domain.input;

import com.tramchester.domain.*;
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

    TransportMode getTransportMode();

    @Deprecated
    int getSeqNumOfFirstStop();

    @Deprecated
    int getSeqNumOfLastStop();

    GraphPropertyKey getProp();

    boolean isFiltered();

    boolean intoNextDay();

    TramTime departTime();

    TramTime arrivalTime();

    @Deprecated
    TramTime earliestDepartTime();

    @Deprecated
    TramTime latestDepartTime();
}
