package com.tramchester.domain.input;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

public interface Trip extends HasId<Trip>, HasTransportMode, GraphProperty, CoreDomain {

    IdFor<Trip> getId();

    StopCalls getStopCalls();

    Service getService();

    String getHeadsign();

    Route getRoute();

    TransportMode getTransportMode();

    GraphPropertyKey getProp();

    boolean isFiltered();

    boolean intoNextDay();

    TramTime departTime();

    TramTime arrivalTime();

    boolean hasStops();
}
