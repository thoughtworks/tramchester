package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ServedRoute {

    private final Set<RouteAndService> routeAndServices;
    private final Map<RouteAndService, TimeRange> timeWindows;

    private final IdSet<Route> routeIds; // for performance, significant
    private final EnumSet<TransportMode> modes; // for performance, significant

    public ServedRoute() {
        routeAndServices = new HashSet<>();
        timeWindows = new HashMap<>();
        routeIds = new IdSet<>();
        modes = EnumSet.noneOf(TransportMode.class);
    }

    public void add(Route route, Service service, TramTime callingTime) {
        RouteAndService routeAndService = new RouteAndService(route, service);
        routeAndServices.add(routeAndService);

        if (callingTime.isValid()) {
            if (timeWindows.containsKey(routeAndService)) {
                timeWindows.get(routeAndService).updateToInclude(callingTime);
            } else {
                timeWindows.put(routeAndService, new TimeRange(callingTime));
            }
        }

        routeIds.add(route.getId());
        modes.add(route.getTransportMode());
    }

    public boolean serves(TransportMode mode) {
        return modes.contains(mode);
    }

    public boolean isEmpty() {
        return routeAndServices.isEmpty();
    }

    /***
     * Use the version that takes a date?
     * @return all the routes
     */
    @Deprecated
    public Set<Route> getRoutes() {
        return routeAndServices.stream().map(RouteAndService::getRoute).collect(Collectors.toSet());
    }


    // TODO Should consider all routes that might run during the time window - and then handle this in the MIN/MAX for
    // the number of hops....
    public Set<Route> getRoutes(LocalDate date, TimeRange range) {
        Set<Route> results = getRouteForDateAndTimeRange(date, range);
        if (range.intoNextDay()) {
            TimeRange nextDayRange = range.forFollowingDay();
            LocalDate followingDay = date.plusDays(1);
            results.addAll(getRouteForDateAndTimeRange(followingDay, nextDayRange));
        }
        return results;
    }

    @NotNull
    private Set<Route> getRouteForDateAndTimeRange(LocalDate date, TimeRange range) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                filter(routeAndService -> timeWindows.get(routeAndService).anyOverlap(range)).
                map(RouteAndService::getRoute).
                collect(Collectors.toSet());
    }

    /***
     * Use the form that takes a date
     * @param route the route
     * @return true if route present
     */
    @Deprecated
    public boolean contains(Route route) {
        return routeIds.contains(route.getId());
    }

    @Override
    public String toString() {
        return "ServedRoute{" +
                "routeAndServices=" + routeAndServices +
                ", timeWindows=" + timeWindows +
                '}';
    }

    public Set<TransportMode> getTransportModes() {
        return Collections.unmodifiableSet(modes);
    }
}
