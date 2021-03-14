package com.tramchester.domain.factory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    public TransportEntityFactoryForTFGM(TramchesterConfig config) {
        super(config);
    }

    @Override
    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {

        IdFor<Route> routeId = createRouteId(routeData.getId());

        // TODO No longer needed?
        String routeName = routeData.getLongName();
        if (config.getRemoveRouteNameSuffix()) {
            int indexOf = routeName.indexOf("(");
            if (indexOf > -1) {
                routeName = routeName.substring(0,indexOf).trim();
            }
        }

        return new Route(routeId, routeData.getShortName().trim(), routeName, agency, TransportMode.fromGTFS(routeType));
    }

    @Override
    public IdFor<Route> createRouteId(IdFor<Route> routeId) {
        // NOTE: tfgm has date suffix at end of route ID i.e. METLPURP:O:2021-03-08
        // remove that so route IDs do not change for each release of data
        String originalId = routeId.forDTO();
        int endId = originalId.lastIndexOf(':')+1;
        int index = Math.min(endId, originalId.length());
        String idWithoutDateSuffix = originalId.substring(0, index);
        return StringIdFor.createId(idWithoutDateSuffix);
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
        boolean isMetrolink = Agency.IsMetrolink(agencyId);
        if (isMetrolink && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            return GTFSTransportationType.tram;
        }

        if ( (routeType==GTFSTransportationType.tram) && (!isMetrolink) ) {
            logger.error("Tram transport type seen for non-metrolink agency for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.bus.name() + " for " + routeData);
            return GTFSTransportationType.bus;
        }

        return routeType;

    }
}
