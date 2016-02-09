package com.tramchester.domain;

public interface VehicleStage extends TransportStage{
    Station getFirstStation();

    Station getLastStation();

    String getRouteName();
}
