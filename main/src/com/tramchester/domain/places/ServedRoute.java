package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServedRoute {

    private final Set<RouteAndService> routeAndServices;
    private final Map<RouteAndService, TimeRange> timeWindows;

    public ServedRoute() {
        routeAndServices = new HashSet<>();
        timeWindows = new HashMap<>();
    }

    public boolean serves(TransportMode mode) {
        return routeAndServices.stream().map(RouteAndService::getRoute).
                anyMatch(route -> route.getTransportMode().equals(mode));
    }

    public boolean isEmpty() {
        return routeAndServices.isEmpty();
    }

    /***
     * Use the version that takes a date
     * @return all the routes
     */
    @Deprecated
    public Set<Route> getRoutes() {
        return routeAndServices.stream().map(RouteAndService::getRoute).collect(Collectors.toSet());
    }


    // TODO Have to handle crossing midnight next day logic
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

    public boolean routeAvailableOnDate(Route route, LocalDate date, TimeRange range) {
        return routeAndServices.stream().
                filter(routeAndService -> routeAndService.isAvailableOn(date) && routeAndService.getRoute().equals(route)).
                map(timeWindows::get).
                anyMatch(timeWindow -> timeWindow.anyOverlap(range));
    }

    /***
     * Use the form that takes a date
     * @param route the route
     * @return true if route present
     */
    @Deprecated
    public boolean contains(Route route) {
        return routeAndServices.stream().map(RouteAndService::getRoute).anyMatch(item -> item.equals(route));
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
    }

    @Override
    public String toString() {
        return "ServedRoute{" +
                "routeAndServices=" + routeAndServices +
                ", timeWindows=" + timeWindows +
                '}';
    }

    public Set<TransportMode> getTransportModes() {
        return routeAndServices.stream().map(RouteAndService::getTransportMode).collect(Collectors.toSet());
    }
}
