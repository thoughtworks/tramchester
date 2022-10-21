package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

    // TODO SUSPENDED, Picc gardens emergency track work
    // https://tfgm.com/piccadilly-gardens-service-change
    //AltrinchamPiccadilly("Purple Line", Inbound, "Altrincham - Piccadilly"),
    //PiccadillyAltrincham("Purple Line", Outbound, "Piccadilly - Altrincham"),

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
    CornbrookTheTraffordCentre("Red Line", Outbound, "Cornbrook - The Trafford Centre"),

    // TODO July 2022 - eccles replacement services - UNTIL October 22nd
    ReplacementRouteToEccles("Blue Line Bus Replacement", Inbound, "Media City Metrolink Replacement - Eccles"),
    ReplacementRouteFromEccles("Blue Line Bus Replacement", Outbound, "Eccles - Media City Metrolink Replacement");

    private final IdFor<Route> fakeId;
    private final RouteDirection direction;
    private final String shortName;
    private final String longName;
    private final boolean isReplacement;

    KnownTramRoute(String shortName, RouteDirection direction, String longName) {
        this.longName = longName;
        this.direction = direction;
        this.shortName = shortName;

        // new format for IDs METLRED:I:xxxxxx
        String idSuffix;
        if (shortName.contains("Replacement") || shortName.contains("Replaement")) { // yep, typo in the source data
            idSuffix = getSuffixFor(shortName);
            this.isReplacement = true;
        } else {
            this.isReplacement = false;
            int endIndex = Math.min(shortName.length(), 4);
            idSuffix = shortName.toUpperCase().substring(0, endIndex).trim();
        }
        this.fakeId = createId(format("METL%s%sCURRENT", idSuffix, direction.getSuffix()));

    }

    public static List<KnownTramRoute> getFor(TramDate when) {
        List<KnownTramRoute> routes = new ArrayList<>(Arrays.asList(KnownTramRoute.values())); // need mutable
        if (when.isAfter(TramDate.of(2022, 10, 22))) {
            routes.remove(ReplacementRouteToEccles);
            routes.remove(ReplacementRouteFromEccles);
        }
        return routes;
    }

    private String getSuffixFor(String shortName) {
        return switch (shortName) {
            case "Blue Line Bus Replacement" -> "ML1";
            case "Yellow Line Replacement Bus" -> "ML2";
            case "Green Line Replacement Bus" -> "ML3";
            case "Purple Line Bus Replaement", "Purple Line Replacement Bus" -> "ML4";
            default -> throw new RuntimeException("Unexpected replacement service short name" + shortName);
        };
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

    public boolean isReplacement() {
        return isReplacement;
    }
}
