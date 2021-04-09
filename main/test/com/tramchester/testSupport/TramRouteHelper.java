package com.tramchester.testSupport;

import com.tramchester.App;
import com.tramchester.ComponentContainer;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.reference.KnownTramRoute;

import java.util.HashMap;
import java.util.Map;

/***
 * Test helper only
 */
public class TramRouteHelper {

    private final Map<KnownTramRoute, Route> map;

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
            map.put(knownRoute, TestEnv.findTramRoute(routeRepository, knownRoute));
        }
    }

    public Route get(KnownTramRoute knownRoute) {
        return map.get(knownRoute);
    }

    public IdFor<Route> getId(KnownTramRoute knownTramRoute) {
        return map.get(knownTramRoute).getId();
    }
}
