package com.tramchester.repository;

import com.tramchester.geo.StationLocations;

public interface TransportDataProvider extends StationLocations.ProvidesStationAddedCallback {
    TransportData getData();
}
