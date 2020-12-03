package com.tramchester.domain.reference;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum KnownRoutes {
    AltrinchamPiccadilly("MET:2:I:"),
    PiccadillyAltrincham("MET:2:O:"),
    intuTraffordCentreCornbrook("MET:7:I:"),
    CornbrookintuTraffordCentre("MET:7:O:"),
    AshtonunderLyneManchesterEccles("MET:3:I:"),
    EcclesManchesterAshtonunderLyne("MET:3:O:"),
    BuryPiccadilly("MET:4:I:"),
    PiccadillyBury("MET:4:O:"),
    EDidsburyManchesterRochdale("MET:5:I:"),
    RochdaleManchesterEDidsbury("MET:5:O:"),
    VictoriaManchesterAirport("MET:6:I:"),
    ManchesterAirportVictoria("MET:6:O:");

    private final IdFor<Route> id;

    KnownRoutes(String id) {
        this.id = createId(id);
    }

    public IdFor<Route> getId() {
        return id;
    }

    public static IdSet<Route> ids;
    public static final Map<IdFor<Route>, KnownRoutes> map;

    static {
        ids = Arrays.stream(KnownRoutes.values()).map(KnownRoutes::getId).collect(IdSet.idCollector());
        map = Arrays.stream(KnownRoutes.values()).map(element -> Pair.of(element.getId(),element)).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private IdFor<Route> createId(String stationId) {
        return IdFor.createId(stationId);
    }

    public boolean matches(Route route) {
        return id.equals(route.getId());
    }
}
