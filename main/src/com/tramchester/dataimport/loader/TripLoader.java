package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TripLoader {
    private static final Logger logger = LoggerFactory.getLogger(TripLoader.class);

    private final WriteableTransportData buildable;
    private final TransportEntityFactory factory;

    public TripLoader(WriteableTransportData buildable, TransportEntityFactory factory) {
        this.buildable = buildable;
        this.factory = factory;
    }

    public TripAndServices load(Stream<TripData> tripDataStream, RouteDataLoader.ExcludedRoutes excludedRoutes) {
        logger.info("Loading trips");
        TripAndServices results = new TripAndServices(factory);

        AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            IdFor<Service> serviceId = tripData.getServiceId();
            IdFor<Route> routeId = factory.createRouteId(tripData.getRouteId());
            IdFor<Trip> tripId = tripData.getTripId();

            if (buildable.hasRouteId(routeId)) {
                Route route = buildable.getMutableRoute(routeId);
                Service service = results.getOrCreateService(serviceId);
                results.createTripIfMissing(tripId, tripData, service, route);
                count.getAndIncrement();
            } else {
                if (!excludedRoutes.wasExcluded(routeId)) {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get() + " trips");
        return results;
    }
}
