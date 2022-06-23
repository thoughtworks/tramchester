package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;

import java.util.HashSet;
import java.util.Set;

public class TripAndServices {
    private final CompositeIdMap<Service, MutableService> services;
    private final CompositeIdMap<Trip, MutableTrip> trips;
    private final Set<String> loadedTrips;

    private final TransportEntityFactory factory;

    public TripAndServices(TransportEntityFactory factory) {
        this.factory = factory;
        services = new CompositeIdMap<>();
        trips = new CompositeIdMap<>();
        loadedTrips = new HashSet<>();
    }

    public void clear() {
        services.clear();
        trips.clear();
        loadedTrips.clear();
    }

    public boolean hasId(IdFor<Trip> id) {
        return trips.hasId(id);
    }

    public MutableTrip getTrip(IdFor<Trip> id) {
        return trips.get(id);
    }

    public MutableService getService(IdFor<Service> id) {
        return services.get(id);
    }

    public MutableService getOrCreateService(IdFor<Service> serviceId) {
        return services.getOrAdd(serviceId, () -> factory.createService(serviceId));
    }

    public void createTripIfMissing(IdFor<Trip> tripId, TripData tripData, MutableService service, Route route, TransportMode transportMode) {
        trips.getOrAdd(tripId, () -> factory.createTrip(tripData, service, route, transportMode));
        loadedTrips.add(tripId.getGraphId());
    }

    public boolean hasId(String tripId) {
        return loadedTrips.contains(tripId);
    }
}
