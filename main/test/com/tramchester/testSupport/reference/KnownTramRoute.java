package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import io.swagger.models.auth.In;

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
    CornbrookTheTraffordCentre("Red Line", Outbound, "Cornbrook - The Trafford Centre"),

    // TODO July 2022 - eccles replacement services
    ReplacementRouteToEccles("Blue Line Bus Replacement", Inbound, "Media City Metrolink Replacement - Eccles"),
    ReplacementRouteFromEccles("Blue Line Bus Replacement", Outbound, "Eccles - Media City Metrolink Replacement");

    // TODO August 13 to 16 2022
    //ReplacementRouteFromBuryToWhitefield("Yellow Line Replacement Bus", Inbound, "Bury Metrolink Replacement - Whitefield"),
    //ReplacementRouteFromWhitefieldToBury("Yellow Line Replacement Bus", Outbound, "Whitefield - Bury Metrolink Replacement"),

    //ReplacementRouteFromCrumpsallToBury("Green Line Replacement Bus", Inbound, "Crumpsall Metrolink Replacement - Bury"),
    //ReplacementRouteFromBuryToCrumpsall("Green Line Replacement Bus", Outbound, "Bury - Crumpsall Metrolink Replacement"),

    // TODO August 17 to 19 2022, note typo is in the source data
    //ReplacementRouteAltrinchamToTimperley("Purple Line Bus Replaement",Inbound, "Altrincham Metrolink Replacement - Timperley"),
    //ReplacementRouteTimperleyToAltrincham("Purple Line Bus Replaement",Outbound, "Timperley - Altrincham Metrolink Replacement"),

    //ReplacementRouteBuryToVictoriaYellow("Yellow Line Replacement Bus",Inbound, "Bury Metrolink Replacement - Victoria"),
    //ReplacementRouteVictoriaToBuryYellow("Yellow Line Replacement Bus", Outbound, "Victoria - Bury Metrolink Replacement"),

    //ReplacementRouteVictoriaToBuryGreen("Green Line Replacement Bus", Outbound,"Victoria Metrolink Replacement - Manchester - Bury"),
    //ReplacementRouteBuryToVictoriaGreen("Green Line Replacement Bus", Inbound, "Bury - Manchester - Victoria Metrolink Replacement");

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
