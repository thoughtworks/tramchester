package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ServedRoute {

    private final Set<RouteAndService> routeAndServices;
    private final Map<RouteAndService, TimeRange> timeWindows;

    private final IdSet<Route> routeIds; // for performance, significant
    private final EnumSet<TransportMode> allServedModes; // for performance, significant

    public ServedRoute() {
        routeAndServices = new HashSet<>();
        timeWindows = new HashMap<>();
        routeIds = new IdSet<>();
        allServedModes = EnumSet.noneOf(TransportMode.class);
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
        allServedModes.add(route.getTransportMode());
    }

    public boolean isEmpty() {
        return routeAndServices.isEmpty();
    }

    // the number of hops....
    public Set<Route> getRoutes(TramDate date, TimeRange range, Set<TransportMode> modes) {
        Set<Route> results = getRouteForDateAndTimeRange(date, range, modes);
        if (range.intoNextDay()) {
            TimeRange nextDayRange = range.forFollowingDay();
            TramDate followingDay = date.plusDays(1);
            results.addAll(getRouteForDateAndTimeRange(followingDay, nextDayRange, modes));
        }
        return results;
    }

    @NotNull
    private Set<Route> getRouteForDateAndTimeRange(TramDate date, TimeRange range, Set<TransportMode> modes) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(date)).
                filter(routeAndService -> timeWindows.get(routeAndService).anyOverlap(range)).
                map(RouteAndService::getRoute).
                filter(route -> modes.contains(route.getTransportMode())).
                collect(Collectors.toSet());
    }


    public boolean anyAvailable(TramDate when, TimeRange timeRange, Set<TransportMode> preferredModes) {
        return routeAndServices.stream().
                filter(routeAndService -> preferredModes.contains(routeAndService.getTransportMode())).
                filter(routeAndService -> routeAndService.isAvailableOn(when)).
                anyMatch(routeAndService -> timeWindows.get(routeAndService).anyOverlap(timeRange));
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
        return Collections.unmodifiableSet(allServedModes);
    }

}
