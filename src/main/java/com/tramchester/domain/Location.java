package com.tramchester.domain;

import com.tramchester.domain.presentation.LatLong;

public interface Location {
    String getId();

    String getName();

    LatLong getLatLong();

    boolean isTram();
}
