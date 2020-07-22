package com.tramchester.domain.places;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public interface Location extends HasTransportMode, IdForDTO {
//    String getId();

    String getName();

    LatLong getLatLong();

    String getArea();

    boolean hasPlatforms();

    List<Platform> getPlatforms();

    TransportMode getTransportMode();

}
