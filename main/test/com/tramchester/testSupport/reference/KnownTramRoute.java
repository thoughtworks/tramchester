package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

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

    private final IdFor<Route> id;
    private final RouteDirection direction;
    private final String shortName;

    KnownTramRoute(String shortName, RouteDirection direction) {
        // new format for IDs METLRED:I:xxxxxx
        int endIndex = Math.min(shortName.length(), 4);
        String idSuffix = shortName.toUpperCase().substring(0, endIndex);
        this.id = createId(format("METL%s%s", idSuffix, direction.getSuffix()));
        this.direction = direction;
        this.shortName = shortName + " Line";
    }

    private StringIdFor<Route> createId(String stationId) {
        return StringIdFor.createId(stationId);
    }

    public RouteDirection direction() {
        return direction;
    }

    public TransportMode mode() { return TransportMode.Tram; }

    public String shortName() {
        return shortName;
    }

    public IdFor<Route> getFakeId() {
        return id;
    }
}
