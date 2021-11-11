package com.tramchester.testSupport;

import com.tramchester.App;
import com.tramchester.ComponentContainer;
import com.tramchester.domain.Agency;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdSet;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownTramRoute;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * Test helper only
 */
public class TramRouteHelper {

    private final Map<KnownTramRoute, Set<RouteReadOnly>> map;

    public TramRouteHelper(ComponentContainer componentContainer) {
        this(componentContainer.get(RouteRepository.class));
    }

    public TramRouteHelper(IntegrationAppExtension appExtension) {
       this(getRepositoryFrom(appExtension));
    }

    private static RouteRepository getRepositoryFrom(IntegrationAppExtension appExtension) {
        App app =  appExtension.getApplication();
        return app.getDependencies().get(RouteRepository.class);
    }

    public TramRouteHelper(RouteRepository routeRepository) {
        map = new HashMap<>();
        createMap(routeRepository);
    }

    private void createMap(RouteRepository routeRepository) {
        KnownTramRoute[] knownTramRoutes = KnownTramRoute.values();
        for (KnownTramRoute knownRoute : knownTramRoutes) {
            final Set<RouteReadOnly> routesByShortName =
                    routeRepository.findRoutesByShortName(Agency.METL, knownRoute.shortName()).
                            stream().filter(found -> found.getId().forDTO().contains(knownRoute.direction().getSuffix())).
                            collect(Collectors.toSet());
            if (routesByShortName.isEmpty()) {
                throw new RuntimeException("Found nothing matching " + knownRoute);
            }
            map.put(knownRoute, routesByShortName);
        }
    }

    public Set<RouteReadOnly> get(KnownTramRoute knownRoute) {
        guard(knownRoute);
        return map.get(knownRoute);
    }

    private void guard(KnownTramRoute knownRoute) {
        if (!map.containsKey(knownRoute)) {
            throw new RuntimeException("Missing " + knownRoute);
        }
    }

    public IdSet<RouteReadOnly> getId(KnownTramRoute knownRoute) {
        guard(knownRoute);
        return map.get(knownRoute).stream().collect(IdSet.collector());
    }
}
