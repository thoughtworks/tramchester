package com.tramchester.domain.reference;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

public enum KnownRoute {
    AltrinchamPiccadilly(2, Inbound),
    PiccadillyAltrincham(2, Outbound),
    AshtonunderLyneManchesterEccles(3, Inbound),
    EcclesManchesterAshtonunderLyne(3, Outbound),
    BuryPiccadilly(4, Inbound),
    PiccadillyBury(4, Outbound),
    EDidsburyManchesterRochdale(5, Inbound),
    RochdaleManchesterEDidsbury(5, Outbound),
    VictoriaManchesterAirport(6, Inbound),
    ManchesterAirportVictoria(6, Outbound),
    intuTraffordCentreCornbrook(7, Inbound),
    CornbrookintuTraffordCentre(7, Outbound);

    private final IdFor<Route> id;
    private final Integer number;
    private final RouteDirection direction;

    KnownRoute(int number, RouteDirection direction) {
        this.id = createId(format("MET:%s%s", number, direction.getSuffix()));
        this.number = number;
        this.direction = direction;
    }

    public IdFor<Route> getId() {
        return id;
    }

    public static IdSet<Route> ids;
    public static final Map<IdFor<Route>, KnownRoute> map;

    static {
        ids = Arrays.stream(KnownRoute.values()).map(KnownRoute::getId).collect(IdSet.idCollector());
        map = Arrays.stream(KnownRoute.values()).map(element -> Pair.of(element.getId(),element)).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private IdFor<Route> createId(String stationId) {
        return IdFor.createId(stationId);
    }

    public boolean matches(Route route) {
        return id.equals(route.getId());
    }

    public String number() {
        return number.toString();
    }

    public RouteDirection direction() {
        return direction;
    }
}
