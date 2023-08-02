package com.tramchester.testSupport.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;

import java.time.DayOfWeek;
import java.util.*;

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

    EastDidisburyManchesterShawandCromptonRochdale("5PNK", Inbound, "East Didsbury - Manchester - Shaw and Crompton - Rochdale"),
    RochdaleShawandCromptonManchesterEastDidisbury("5PNK", Outbound, "Rochdale - Shaw and Crompton - Manchester - East Didsbury"),

    ManchesterAirportWythenshaweVictoria("6NVY", Inbound, "Manchester Airport - Wythenshawe - Victoria"),
    VictoriaWythenshaweManchesterAirport("6NVY", Outbound, "Victoria - Wythenshawe - Manchester Airport"),

    TheTraffordCentreCornbrook("7RED", Inbound, "The Trafford Centre - Cornbrook"),
    CornbrookTheTraffordCentre("7RED", Outbound, "Cornbrook - The Trafford Centre");

    private final IdFor<Route> fakeId;
    private final RouteDirection direction;
    private final String shortName;
    private final String longName;

    public static Set<KnownTramRoute> getFor(TramDate date) {
        EnumSet<KnownTramRoute> routes = EnumSet.noneOf(KnownTramRoute.class);

        TramDate lateMayBankHol2023 = TramDate.of(2023,5,29);

        // do it this way so can tune based on closures etc.
        routes.add(ManchesterAirportWythenshaweVictoria);
        routes.add(VictoriaWythenshaweManchesterAirport);
        routes.add(TheTraffordCentreCornbrook);
        routes.add(CornbrookTheTraffordCentre);
        routes.add(EastDidisburyManchesterShawandCromptonRochdale);
        routes.add(RochdaleShawandCromptonManchesterEastDidisbury);
        routes.add(AshtonUnderLyneManchesterEccles);
        routes.add(EcclesManchesterAshtonUnderLyne);
        routes.add(BuryPiccadilly);
        routes.add(PiccadillyBury);
        routes.add(AltrinchamPiccadilly);
        routes.add(PiccadillyAltrincham);

//        routes.add(BuryManchesterAltrincham);
//        routes.add(AltrinchamManchesterBury);

        // nothing on tfgm website about this, but routes not present for sundays.....?
        if (! (date.getDayOfWeek().equals(DayOfWeek.SUNDAY) || date.equals(lateMayBankHol2023))) {
            routes.add(BuryManchesterAltrincham);
            routes.add(AltrinchamManchesterBury);
        }

        return routes;
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
        return Route.createId(stationId);
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

}
