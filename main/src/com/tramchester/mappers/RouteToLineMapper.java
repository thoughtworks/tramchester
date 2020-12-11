package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.liveUpdates.LineDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.repository.TramCentralZoneDirectionRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static java.lang.String.format;

@LazySingleton
public class RouteToLineMapper {
    private static final Logger logger = LoggerFactory.getLogger(RouteToLineMapper.class);

    private final TramCentralZoneDirectionRespository tramCentralZoneDirectionRespository;

    //private final String ExchangeSquareId = "9400ZZMAEXS";

    @Inject
    public RouteToLineMapper(TramCentralZoneDirectionRespository tramCentralZoneDirectionRespository) {
        this.tramCentralZoneDirectionRespository = tramCentralZoneDirectionRespository;
    }

    public LineAndDirection map(RouteStation routeStation) {
        Route route = routeStation.getRoute();
        if (!KnownRoute.ids.contains(route.getId())) {
            logger.error("Unknown route " + route.getId());
            return LineAndDirection.Unknown;
        }

//        if (ExchangeSquareId.equals(routeStation.getStation().getId().forDTO())) {
//            return mapLineDirection(Lines.Eccles, route);
//        }

        if (CentralZoneStation.contains(routeStation.getStation())) {
            return mapDirect(routeStation);
        }

        KnownRoute knownRoute = KnownRoute.map.get(route.getId());
        switch (knownRoute) {
            case RochdaleManchesterEDidsbury:
                return mapCrossesCentralZone(routeStation, Lines.OldhamAndRochdale, Lines.SouthManchester);
            case EDidsburyManchesterRochdale:
                return mapCrossesCentralZone(routeStation, Lines.SouthManchester, Lines.OldhamAndRochdale);
            case EcclesManchesterAshtonunderLyne:
                return mapCrossesCentralZone(routeStation, Lines.Eccles, Lines.EastManchester);
            case AshtonunderLyneManchesterEccles:
                return mapCrossesCentralZone(routeStation, Lines.EastManchester, Lines.Eccles);
            case ManchesterAirportVictoria:
            case VictoriaManchesterAirport:
                return mapLineDirection(Lines.Airport, route);
            case PiccadillyAltrincham:
            case AltrinchamPiccadilly:
                return mapLineDirection(Lines.Altrincham, route);
            case PiccadillyBury:
            case BuryPiccadilly:
                return mapLineDirection(Lines.Bury, route);
            case CornbrookintuTraffordCentre:
            case intuTraffordCentreCornbrook:
                return mapLineDirection(Lines.TraffordPark, route);
            default:
                logger.warn("Failed for map route " + knownRoute + " for " + routeStation);
                return LineAndDirection.Unknown;
        }
    }

    private LineAndDirection mapLineDirection(Lines line, Route route) {
        boolean inboundRoute = route.getRouteDirection() == RouteDirection.Inbound;
        LineDirection lineDirection = inboundRoute ? LineDirection.Incoming : LineDirection.Outgoing;

        if (line==Lines.Eccles || line==Lines.OldhamAndRochdale || line==Lines.Airport) {
            return new LineAndDirection(line,LineDirection.Reverse(lineDirection));
        }
        return new LineAndDirection(line, lineDirection);
    }

    private LineAndDirection mapCrossesCentralZone(RouteStation routeStation, Lines towardsLine, Lines awayLine) {

        TramCentralZoneDirectionRespository.Place place = tramCentralZoneDirectionRespository.getStationPlacement(routeStation);

        if (place == TramCentralZoneDirectionRespository.Place.towards) {
            return mapLineDirection(towardsLine,routeStation.getRoute());
        }
        if (place == TramCentralZoneDirectionRespository.Place.away) {
            return mapLineDirection(awayLine, routeStation.getRoute());
        }
        logger.warn(format("Route %s Failed to determine line for '%s' and station %s",
                routeStation.getRoute().getName(), place, routeStation));
        return LineAndDirection.Unknown;
    }


    private LineAndDirection mapDirect(RouteStation routeStation) {
        CentralZoneStation multilineStation = CentralZoneStation.map.get(routeStation.getStationId());
        Route route = routeStation.getRoute();
        return mapLineDirection(multilineStation.getLine(), route);
    }

}
