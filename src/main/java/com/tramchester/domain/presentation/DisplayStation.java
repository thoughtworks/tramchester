package com.tramchester.domain.presentation;


import com.tramchester.domain.Station;

public class DisplayStation extends Station {
    private String proximityGroup;

    public DisplayStation() {
        // deserialization
    }

    public DisplayStation(Station other, String proximityGroup) {
        super(other);
        this.proximityGroup = proximityGroup;
    }

    public String getProximityGroup() {
        return proximityGroup;
    }

}
