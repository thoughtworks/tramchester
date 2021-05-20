package com.tramchester.domain.factory;

import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    private final NaptanRespository naptanRespository;

    public TransportEntityFactoryForTFGM(NaptanRespository naptanRespository) {
        super();
        this.naptanRespository = naptanRespository;
    }

    @Override
    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {

        IdFor<Route> routeId = createRouteId(routeData.getId());
        String routeName = routeData.getLongName();
        return new Route(routeId, routeData.getShortName().trim(), routeName, agency, TransportMode.fromGTFS(routeType));
    }

    @Override
    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {
        String area = stopData.getArea();
        if (naptanRespository.isEnabled()) {
            if (naptanRespository.contains(stationId)) {
                area = getAreaFromNaptanData(stationId);
            } else {
                logger.warn("No naptap data found for " + stationId);
            }
        }

        // Check for duplicate names - handled by CompositeStationRepository
        return new Station(stationId, area, workAroundName(stopData.getName()),
                stopData.getLatLong(), position);
    }

    private String getAreaFromNaptanData(IdFor<Station> stationId) {
        String area;
        StopsData naptapData = naptanRespository.get(stationId);
        area = naptapData.getLocalityName();
        String parent = naptapData.getParentLocalityName();
        if (!parent.isBlank()) {
            area = area + ", " + parent;
        }
        return area;
    }

    // spelt different ways within data
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
