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

    public TramRouteHelper() {
        map = null;
    }

    private void createMap(RouteRepository routeRepository) {
        map = new HashMap<>();
        KnownTramRoute[] knownTramRoutes = KnownTramRoute.values();
        for (KnownTramRoute knownRoute : knownTramRoutes) {
            final Set<Route> routesByShortName =
                    routeRepository.
                            findRoutesByShortName(MutableAgency.METL, knownRoute.shortName()).
                            stream().
                            filter(found -> found.getId().forDTO().contains(knownRoute.direction().getSuffix())).
                            collect(Collectors.toSet());
            if (routesByShortName.isEmpty()) {
                throw new RuntimeException("Found nothing matching " + knownRoute);
            }
            map.put(knownRoute, routesByShortName);
        }
    }

    /***
     * Note: Use version that takes a date to get more consistent results
     * @param knownRoute the route to find
     * @param routeRepository the repository
     * @return set of matching routes
     */
    @Deprecated
    public Set<Route> get(KnownTramRoute knownRoute, RouteRepository routeRepository) {
        guard(knownRoute, routeRepository);
        return map.get(knownRoute);
    }

    public Route getOneRoute(KnownTramRoute knownRoute, RouteRepository routeRepository, TramDate date) {
        guard(knownRoute, routeRepository);
        List<Route> result = map.get(knownRoute).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toList());
        if (result.size()>1) {
            throw new RuntimeException(format("Found two many routes matching date %s and known route %s", date, knownRoute));
        }
        return result.get(0);
    }

    @Deprecated
    public Route getOneRoute(KnownTramRoute knownRoute, RouteRepository routeRepository, LocalDate date) {
        return getOneRoute(knownRoute, routeRepository, TramDate.of(date));
    }

    public IdSet<Route> getId(KnownTramRoute knownRoute, RouteRepository routeRepository) {
        guard(knownRoute, routeRepository);
        return map.get(knownRoute).stream().collect(IdSet.collector());
    }


    private void guard(KnownTramRoute knownRoute, RouteRepository routeRepsoitory) {
        if (map==null) {
            createMap(routeRepsoitory);
        }
        if (!map.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }
}
