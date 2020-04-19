package com.tramchester.domain.places;

import com.tramchester.domain.HasId;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public interface Location extends HasId {
    String getId();

    String getName();

    LatLong getLatLong();

    boolean isTram();

    String getArea();

    boolean hasPlatforms();

    List<Platform> getPlatforms();
}
