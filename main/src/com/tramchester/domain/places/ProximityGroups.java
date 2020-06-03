package com.tramchester.domain.places;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.ProximityGroup;

import java.util.Arrays;
import java.util.List;

public class ProximityGroups {

    private final TramchesterConfig config;

    // TODO enum
    public static final ProximityGroup MY_LOCATION = new ProximityGroup(1,"Nearby");
    public static final ProximityGroup RECENT = new ProximityGroup(2,"Recent");
    public static final ProximityGroup NEAREST_STOPS = new ProximityGroup(3,"Nearest Stops");
    public static final ProximityGroup STOPS = new ProximityGroup(4,"All Stops");
    public static final ProximityGroup POSTCODES = new ProximityGroup(5,"Postcodes");

    public ProximityGroups(TramchesterConfig config) {
        this.config = config;
    }

    public List<ProximityGroup> getGroups() {
        if (config.getBus()) {
            return Arrays.asList(MY_LOCATION,RECENT,NEAREST_STOPS, STOPS, POSTCODES);
        } else {
            return Arrays.asList(MY_LOCATION,RECENT,NEAREST_STOPS, STOPS);
        }
    }
}
