package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.Direction;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteToLineMapper {
    private static final Logger logger = LoggerFactory.getLogger(RouteToLineMapper.class);

    private enum MultilineStations {
        CORNBROOK("9400ZZMACRN"),
        ST_PETERS_SQUARE("9400ZZMASTP"),
        PIC_GARDENS("9400ZZMAPGD"),
        TRAF_BAR("9400ZZMATRA"),
        ST_WS_ROAD("9400ZZMASTW"),
        VICTORIA("9400ZZMAVIC"),
        DEANSGATE("9400ZZMAGMX"),
        PICCADILLY("9400ZZMAPIC"),
        EXCHANGE_SQUARE(""),
        MARKET_STREET(""),
        SHUDEHILL("9400ZZMASHU");

        private final String stationId;

        MultilineStations(String stationId) {
            this.stationId = stationId;
        }

        private boolean matches(Station station) {
            return stationId.equals(station.getId().forDTO());
        }
    }

    public LineAndDirection map(RouteStation routeStation) {

        Station station = routeStation.getStation();
        if (MultilineStations.SHUDEHILL.matches(station)) {
            // TODO Direction

            return new LineAndDirection(Lines.Altrincham, Direction.Unknown);
        }

        String routeName = routeStation.getRoute().getName();
        switch (routeName) {
            case "Piccadilly - Bury":
                return new LineAndDirection(Lines.Bury, Direction.Outgoing);
            case "Bury - Piccadilly":
                return new LineAndDirection(Lines.Bury, Direction.Incoming);
            case "Ashton-under-Lyne - Manchester - Eccles":
                return new LineAndDirection(Lines.EastManchester, Direction.Incoming);
            default:
                logger.warn("Unable to map route to line & direction, route was '" + routeName + "' " + routeStation.getId());
                return LineAndDirection.Unknown;
        }
    }
}
