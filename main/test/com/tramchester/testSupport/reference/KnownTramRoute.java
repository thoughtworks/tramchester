package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {
    AltrinchamPiccadilly("2PPL", Inbound, "Altrincham - Piccadilly"),
    PiccadillyAltrincham("2PPL", Outbound, "Piccadilly - Altrincham"),

    AltrinchamManchesterBury("1GRN", Inbound, "Altrincham - Manchester - Bury"),
    BuryManchesterAltrincham("1GRN", Outbound, "Bury - Manchester - Altrincham"),

    AshtonUnderLyneManchesterEccles("3BLU", Inbound, "Ashton Under Lyne - Manchester - Eccles"),
    EcclesManchesterAshtonUnderLyne("3BLU", Outbound, "Eccles - Manchester - Ashton Under Lyne"),

    BuryPiccadilly("4YLW", Inbound,"Bury - Piccadilly"),
    PiccadillyBury("4YLW", Outbound, "Piccadilly - Bury"),

    EastDidisburyManchesterShawandCromptonRochdale("5PNK", Inbound, "East Didisbury - Manchester - Shaw and Crompton - Rochdale"),
    RochdaleShawandCromptonManchesterEastDidisbury("5PNK", Outbound, "Rochdale - Shaw and Crompton - Manchester - East Didisbury"),

    ManchesterAirportWythenshaweVictoria("6NVY", Inbound, "Manchester Airport - Wythenshawe - Victoria"),
    VictoriaWythenshaweManchesterAirport("6NVY", Outbound, "Victoria - Wythenshawe - Manchester Airport"),

    TheTraffordCentreCornbrook("7RED", Inbound, "The Trafford Centre - Cornbrook"),
    CornbrookTheTraffordCentre("7RED", Outbound, "Cornbrook - The Trafford Centre");

    private final IdFor<Route> fakeId;
    private final RouteDirection direction;
    private final String shortName;
    private final String longName;

    KnownTramRoute(String shortName, RouteDirection direction, String longName) {
        this.longName = longName;
        this.direction = direction;
        this.shortName = shortName;

        // new format for IDs METLRED:I:xxxxxx
        int endIndex = Math.min(shortName.length(), 4);
        String idSuffix = shortName.toUpperCase().substring(0, endIndex);
        this.fakeId = createId(format("METL%s%s", idSuffix, direction.getSuffix()));
    }

    private IdFor<Route> createId(String stationId) {
        return StringIdFor.createId(stationId);
    }

    public RouteDirection direction() {
        return direction;
    }

    public TransportMode mode() { return TransportMode.Tram; }

    /**
     * use with care for tfgm, is duplicated and needs to be combined with RouteDirection
     * @return short name for a route
     */
    public String shortName() {
        return shortName;
    }

    public IdFor<Route> getFakeId() {
        return fakeId;
    }

    @Override
    public String toString() {
        return "KnownTramRoute{" +
                "id=" + fakeId +
                ", direction=" + direction +
                ", shortName='" + shortName + '\'' +
                "} " + super.toString();
    }

    public String longName() {
        return longName;
    }

}
