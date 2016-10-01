package com.tramchester.domain.presentation;


import com.tramchester.domain.Station;

public class DisplayStation extends Station {
    private ProximityGroup proximityGroup;

    public DisplayStation() {
        // deserialization
    }

    public DisplayStation(Station other, ProximityGroup proximityGroup) {
        super(other);
        this.proximityGroup = proximityGroup;
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

}
