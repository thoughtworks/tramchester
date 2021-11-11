package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportEntityFactoryForGBRail extends TransportEntityFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForGBRail.class);

    private static final String FROM = "from ";
    private static final String TO = " to ";

    public TransportEntityFactoryForGBRail() {
        super();
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.gbRail;
    }

    @Override
    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {
        IdFor<Route> routeId = routeData.getId();

        String name = routeData.getLongName();

        name = expandRouteNameFor(name, allStations, agency);

        return new MutableRoute(routeId, routeData.getShortName().trim(), name, agency, TransportMode.fromGTFS(routeType));
    }

    @Override
    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        GTFSTransportationType routeType = routeData.getRouteType();

        if (routeType.equals(GTFSTransportationType.aerialLift) && routeData.getLongName().contains("replacement bus service")) {
            logger.warn("Route has incorrect transport type for replacement bus service, will set to replacementBus. Route: " + routeData);
            return GTFSTransportationType.replacementBus;
        }
        return routeType;
    }

    @Override
    public IdFor<Station> formStationId(String stopId) {
        return StringIdFor.createId(stopId);
    }

    @Override
    public void updateStation(Station station, StopData stopData) {
        logger.error("Did not expect to see stop with same ID again for " + stopData);
    }

    private String expandRouteNameFor(String original, IdMap<Station> allStations, Agency agency) {
        //
        // many train routes names have formats:
        // "<AGENCY_ID> train service from <STATIONID> to <STATIONID>"
        // OR
        // "<AGENCY_ID> replacement bus service from <STATIONID> to <STATIONID>"
        //
        String target = original;

        // agency name
        String agencyId = agency.getId().forDTO();
        String trainPrefix = agencyId + " train service";
        String busPrefix = agencyId + " replacement bus service";
        if (original.startsWith(trainPrefix) || original.startsWith(busPrefix)) {
            target = original.replace(agencyId, agency.getName());
        }

        // station names
        int indexOfFrom = target.indexOf(FROM);
        int indexOfTo = target.indexOf(TO);
        if (indexOfFrom>0 && indexOfTo>0) {
            String from = target.substring(indexOfFrom + FROM.length(), indexOfTo);
            String to = target.substring(indexOfTo + TO.length());
            IdFor<Station> fromId = StringIdFor.createId(from);
            IdFor<Station> toId = StringIdFor.createId(to);

            if (allStations.hasId(toId) && allStations.hasId(fromId)) {
                String toName = allStations.get(toId).getName();
                String fromName = allStations.get(fromId).getName();
                target = target.substring(0, indexOfFrom) + "from " + fromName + " to " + toName;
            }
            logger.debug("Mapped route name form '" + original + "' to '" + target + "'");
        } else {
            logger.warn("Train route name format unrecognised " + original);
        }
        return target;

    }
}
