package com.tramchester.domain.factory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdMap;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    public TransportEntityFactoryForTFGM(TramchesterConfig config) {
        super(config);
    }

    @Override
    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {
        IdFor<Route> routeId = routeData.getId();

        String routeName = routeData.getLongName();
        if (config.getRemoveRouteNameSuffix()) {
            int indexOf = routeName.indexOf("(");
            if (indexOf > -1) {
                routeName = routeName.substring(0,indexOf).trim();
            }
        }

        return new Route(routeId, routeData.getShortName().trim(), routeName, agency,
                TransportMode.fromGTFS(routeType), routeData.getRouteDirection());

    }

    @Override
    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {
        return new Station(stationId, stopData.getArea(), workAroundName(stopData.getName()),
                stopData.getLatLong(), position);
    }

    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    @Override
    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        // NOTE: this data issue has been reported to TFGM
        GTFSTransportationType routeType = routeData.getRouteType();
        if (Agency.IsMetrolink(agencyId) && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            return GTFSTransportationType.tram;
        } else {
            return routeType;
        }
    }
}
