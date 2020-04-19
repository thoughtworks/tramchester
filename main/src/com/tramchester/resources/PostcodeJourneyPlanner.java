package com.tramchester.resources;

import com.tramchester.domain.Journey;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.search.JourneyRequest;

import java.util.stream.Stream;

public class PostcodeJourneyPlanner {
    private final LocationJourneyPlanner planner;

    public PostcodeJourneyPlanner(LocationJourneyPlanner planner) {
        this.planner = planner;
    }

    public Stream<Journey> quickestRouteForLocation(PostcodeLocation postcodeLocation, Station destination, JourneyRequest journeyRequest) {
        LatLong latLong = postcodeLocation.getLatLong();
        return planner.quickestRouteForLocation(latLong, destination, journeyRequest);
    }

    public Stream<Journey> quickestRouteForLocation(Station destination, PostcodeLocation postcodeLocation, JourneyRequest journeyRequest) {
        LatLong latLong = postcodeLocation.getLatLong();
        return planner.quickestRouteForLocation(destination.getId(), latLong, journeyRequest);
    }
}
