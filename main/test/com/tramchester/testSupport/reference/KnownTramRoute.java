package com.tramchester.testSupport.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

import java.util.*;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.RouteDirection.Outbound;
import static java.lang.String.format;

/**
 * see also TramRouteHelper
 * Note: these are validated against tfgm data as part of Integration tests
 */
public enum KnownTramRoute {

    // TODO SUSPENDED???, Picc gardens emergency track work
    // https://tfgm.com/piccadilly-gardens-service-change
    AltrinchamPiccadilly("Purple Line", Inbound, "Altrincham - Piccadilly"),
    PiccadillyAltrincham("Purple Line", Outbound, "Piccadilly - Altrincham"),

    AltrinchamManchesterBury("Green Line", Inbound, "Altrincham - Manchester - Bury"),
    BuryManchesterAltrincham("Green Line", Outbound, "Bury - Manchester - Altrincham"),

    AshtonUnderLyneManchesterEccles("Blue Line", Inbound, "Ashton Under Lyne - Manchester - Eccles"), // Ashton Under Lyne -
    EcclesManchesterAshtonUnderLyne("Blue Line", Outbound, "Eccles - Manchester- Ashton Under Lyne"), //  - Ashton Under Lyne

    BuryPiccadilly("Yellow Line", Inbound,"Bury - Piccadilly"),
    PiccadillyBury("Yellow Line", Outbound, "Piccadilly - Bury"),

    EastDidisburyManchesterShawandCromptonRochdale("Pink Line", Inbound, "East Didisbury - Manchester - Shaw and Crompton - Rochdale"),
    RochdaleShawandCromptonManchesterEastDidisbury("Pink Line", Outbound, "Rochdale - Shaw and Crompton - Manchester - East Didisbury"),

    ManchesterAirportWythenshaweVictoria("Navy Line", Inbound, "Manchester Airport - Wythenshawe - Victoria"),
    VictoriaWythenshaweManchesterAirport("Navy Line", Outbound, "Victoria - Wythenshawe - Manchester Airport"),

    TheTraffordCentreCornbrook("Red Line", Inbound, "The Trafford Centre - Cornbrook"),
    CornbrookTheTraffordCentre("Red Line", Outbound, "Cornbrook - The Trafford Centre"),

    // TODO Piccadilly gardens closure replacement bus
    ReplacementRoutePiccadillyDeansgate("Blue Line Bus Replacement", Inbound,"Piccadilly Station Metrolink Replacement - Castlefield - Deansgate"),
    ReplacementRouteDeansgatePiccadilly("Blue Line Bus Replacement", Outbound, "Deansgate - Castlefield - Piccadilly Station Metrolink Replacement"),

    // TODO Victoria works?
    ManchesterEccles("Blue Line", Inbound,"Manchester - Eccles"),
    EcclesManchester("Blue Line", Outbound, "Eccles - Manchester"),

    AshtonCrumpsall("Yellow Line", Outbound, "Ashton - Crumpsall"),
    CrumpsallAshton("Yellow Line", Inbound, "Crumpsall - Ashton");

    private final IdFor<Route> fakeId;
    private final RouteDirection direction;
    private final String shortName;
    private final String longName;

    public static Set<KnownTramRoute> getFor(TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.allOf(KnownTramRoute.class);

        routes.remove(ReplacementRouteDeansgatePiccadilly);
        routes.remove(ReplacementRoutePiccadillyDeansgate);

        DateRange piccGardensWorkA = DateRange.of(TramDate.of(2022, 11,20), TramDate.of(2022, 11, 30));
        if (piccGardensWorkA.contains(date)) {
            routes.remove(AshtonCrumpsall);
            routes.remove(CrumpsallAshton);
            routes.remove(AltrinchamPiccadilly);
            routes.remove(PiccadillyAltrincham);
            routes.remove(BuryPiccadilly);
            routes.remove(PiccadillyBury);
            routes.remove(AshtonUnderLyneManchesterEccles);
            routes.remove(EcclesManchesterAshtonUnderLyne);
            routes.add(ReplacementRoutePiccadillyDeansgate);
            routes.add(ReplacementRouteDeansgatePiccadilly);
        }

        DateRange piccGardensWorkB = DateRange.of(TramDate.of(2022, 11,21), TramDate.of(2022, 11, 21));
        if (piccGardensWorkB.contains(date)) {
            routes.add(AshtonCrumpsall);
        }

        return routes;
    }

    public static boolean isReplacement(KnownTramRoute knownTramRoute) {
        return switch (knownTramRoute) {
            case ReplacementRouteDeansgatePiccadilly, ReplacementRoutePiccadillyDeansgate, AshtonCrumpsall, CrumpsallAshton,
                    EcclesManchester, ManchesterEccles -> true;
            default -> false;
        };
    }

    KnownTramRoute(String shortName, RouteDirection direction, String longName) {
        this.longName = longName;
        this.direction = direction;
        this.shortName = shortName;

        // new format for IDs METLRED:I:xxxxxx
        String idSuffix;
        if (shortName.contains("Replacement") || shortName.contains("Replaement")) { // yep, typo in the source data
            idSuffix = getSuffixFor(shortName);
        } else {
            int endIndex = Math.min(shortName.length(), 4);
            idSuffix = shortName.toUpperCase().substring(0, endIndex).trim();
        }
        this.fakeId = createId(format("METL%s%sCURRENT", idSuffix, direction.getSuffix()));

    }

    public static int numberOn(TramDate date) {
        return getFor(date).size();
    }

    private String getSuffixFor(String shortName) {
        return switch (shortName) {
            case "Blue Line Bus Replacement" -> "MLDP";
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



    public String longName() {
        return longName;
    }

    public boolean isReplacement() {
        return isReplacement(this);
    }
}
