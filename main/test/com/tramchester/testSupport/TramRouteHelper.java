package com.tramchester.testSupport;

import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownTramRoute;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/***
 * Test helper only
 */
public class TramRouteHelper {

    private Map<KnownTramRoute, Set<Route>> map;
    private final RouteRepository routeRepository;

    public TramRouteHelper(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
        createMap();
    }

    private void createMap() {
        map = new HashMap<>();
        KnownTramRoute[] knownTramRoutes = KnownTramRoute.values(); // ignores date
        for (KnownTramRoute knownRoute : knownTramRoutes) {
            final Set<Route> routesByShortName =
                    routeRepository.
                            findRoutesByShortName(MutableAgency.METL, knownRoute.shortName()).
                            stream().
                            filter(found -> found.getId().forDTO().contains(knownRoute.direction().getSuffix())).
                            collect(Collectors.toSet());
            map.put(knownRoute, routesByShortName);
        }
    }

    /***
     * Note: Use version that takes a date to get more consistent results
     * @param knownRoute the route to find
     * @return set of matching routes
     */
    @Deprecated
    public Set<Route> get(KnownTramRoute knownRoute) {
        guard(knownRoute);
        return map.get(knownRoute);
    }

    public Route getOneRoute(KnownTramRoute knownRoute, TramDate date) {
        guard(knownRoute);
        List<Route> result = map.get(knownRoute).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toList());
        if (result.size()>1) {
            throw new RuntimeException(format("Found two many routes matching date %s and known route %s", date, knownRoute));
        }
        if (result.isEmpty()) {
            throw new RuntimeException(format("Found no routes matching date %s and known route %s", date, knownRoute));
        }
        return result.get(0);
    }

    public IdSet<Route> getId(KnownTramRoute knownRoute) {
        guard(knownRoute);
        return map.get(knownRoute).stream().collect(IdSet.collector());
    }


    private void guard(KnownTramRoute knownRoute) {
        if (!map.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }
}
