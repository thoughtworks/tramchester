package com.tramchester.domain.reference;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.Route;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

// Note: these are validated against tfgm data as part of Integration tests
public enum KnownTramRoute {
    AltrinchamPiccadilly("Purple", Inbound),
    PiccadillyAltrincham("Purple", Outbound),
    AshtonUnderLyneManchesterEccles("Blue", Inbound),
    EcclesManchesterAshtonUnderLyne("Blue", Outbound),
    BuryPiccadilly("Yellow", Inbound),
    PiccadillyBury("Yellow", Outbound),
    EastDidisburyManchesterShawandCromptonRochdale("Pink", Inbound),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink", Outbound),
    VictoriaWythenshaweManchesterAirport("Navy", Outbound),
    ManchesterAirportWythenshaweVictoria("Navy", Inbound),
    TheTraffordCentreCornbrook("Red", Inbound),
    CornbrookTheTraffordCentre("Red", Outbound);

    public static IdSet<Route> ids;
    public static final Map<IdFor<Route>, KnownTramRoute> map;

    static {
        ids = Arrays.stream(KnownTramRoute.values()).map(KnownTramRoute::getId).collect(IdSet.idCollector());
        map = Arrays.stream(KnownTramRoute.values()).map(element -> Pair.of(element.getId(),element)).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private final IdFor<Route> id;
    private final RouteDirection direction;
    private final String shortName;

    KnownTramRoute(String shortName, RouteDirection direction) {
        // new format for IDs METLRED:I:
        int endIndex = Math.min(shortName.length(), 4);
        String idSuffix = shortName.toUpperCase().substring(0, endIndex);
        this.id = createId(format("METL%s%s", idSuffix, direction.getSuffix()));
        this.direction = direction;
        this.shortName = shortName + " Line";
    }

    public IdFor<Route> getId() {
        return id;
    }

    private StringIdFor<Route> createId(String stationId) {
        return StringIdFor.createId(stationId);
    }

    public boolean matches(Route route) {
        return id.equals(route.getId());
    }

    public RouteDirection direction() {
        return direction;
    }

    public TransportMode mode() { return TransportMode.Tram; }

    public String shortName() {
        return shortName;
    }
}
