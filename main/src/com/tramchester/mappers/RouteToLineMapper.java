package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.Direction;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.MultilineStations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteToLineMapper {
    private static final Logger logger = LoggerFactory.getLogger(RouteToLineMapper.class);

    public LineAndDirection map(RouteStation routeStation) {

        Station station = routeStation.getStation();
        if (MultilineStations.Shudehill.matches(station)) {
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
