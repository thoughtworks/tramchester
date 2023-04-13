package com.tramchester.domain;

import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;

public interface HasTransportModes {
    EnumSet<TransportMode> getTransportModes();

}
