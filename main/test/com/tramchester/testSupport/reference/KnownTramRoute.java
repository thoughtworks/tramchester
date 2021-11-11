package com.tramchester.testSupport.reference;

import com.tramchester.domain.RouteReadOnly;
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
    AltrinchamPiccadilly("Purple Line", Inbound, "Altrincham - Piccadilly"),
    PiccadillyAltrincham("Purple Line", Outbound, "Piccadilly - Altrincham"),

    AltrinchamManchesterBury("Green Line", Inbound, "Altrincham - Manchester - Bury"),
    BuryManchesterAltrincham("Green Line", Outbound, "Bury - Manchester - Altrincham"),

    AshtonUnderLyneManchesterEccles("Blue Line", Inbound, "Ashton Under Lyne - Manchester - Eccles"),
    EcclesManchesterAshtonUnderLyne("Blue Line", Outbound, "Eccles - Manchester - Ashton Under Lyne"),

    BuryPiccadilly("Yellow Line", Inbound,"Bury - Piccadilly"),
    PiccadillyBury("Yellow Line", Outbound, "Piccadilly - Bury"),

    EastDidisburyManchesterShawandCromptonRochdale("Pink Line", Inbound, "East Didisbury - Manchester - Shaw and Crompton - Rochdale"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", Outbound, "Rochdale - Shaw and Crompton - Manchester - East Didisbury"),

    ManchesterAirportWythenshaweVictoria("Navy Line", Inbound, "Manchester Airport - Wythenshawe - Victoria"),
    VictoriaWythenshaweManchesterAirport("Navy Line", Outbound, "Victoria - Wythenshawe - Manchester Airport"),

    TheTraffordCentreCornbrook("Red Line", Inbound, "The Trafford Centre - Cornbrook"),
    CornbrookTheTraffordCentre("Red Line", Outbound, "Cornbrook - The Trafford Centre");

    private final IdFor<RouteReadOnly> fakeId;
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

    private IdFor<RouteReadOnly> createId(String stationId) {
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

    public IdFor<RouteReadOnly> getFakeId() {
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
