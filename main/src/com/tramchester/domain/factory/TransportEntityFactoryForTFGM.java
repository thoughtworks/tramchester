package com.tramchester.domain.factory;

import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRespository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {

    private static final String METROLINK_ID_PREFIX = "9400ZZ";
    private static final String METROLINK_NAME_POSTFIX = "(Manchester Metrolink)";

    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    private final NaptanRespository naptanRespository;

    public TransportEntityFactoryForTFGM(NaptanRespository naptanRespository) {
        super();
        this.naptanRespository = naptanRespository;
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.tfgm;
    }

    @Override
    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {

        IdFor<Route> routeId = createRouteId(routeData.getId());
        String routeName = routeData.getLongName();
        return new Route(routeId, routeData.getShortName().trim(), routeName, agency, TransportMode.fromGTFS(routeType));
    }

    @Override
    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        String area = getAreaFor(stationId);

        // NOTE: Tram data has unique positions for each platform
        // TODO What is the right position to use for a tram station?
        final String stationName = createStationName(stopData);
        final Station station = new Station(stationId, area, workAroundName(stationName), stopData.getLatLong(), position,
                getDataSourceId());

        // metrolink tram station, has platforms
        addPlatformIfMissing(stopData, station);

        // Check for duplicate names - handled by CompositeStationRepository
        return station;
    }

    @Override
    public void updateStation(Station station, StopData stopData) {
        addPlatformIfMissing(stopData, station);
    }

    private void addPlatformIfMissing(StopData stopData, Station station) {
        if (isMetrolinkTram(stopData)) {
            Platform platform = new Platform(stopData.getId(), createStationName(stopData), stopData.getLatLong());
            if (!station.getPlatforms().contains(platform)) {
                station.getBuilder().addPlatform(platform);
            }
        }
    }

    private String createStationName(StopData stopData) {
        String text = stopData.getName();
        text = text.replace("\"", "").trim();

        if (text.endsWith(METROLINK_NAME_POSTFIX)) {
            return text.replace(METROLINK_NAME_POSTFIX,"").trim();
        } else {
            return text;
        }
    }

    protected String getAreaFor(IdFor<Station> stationId) {
        if (naptanRespository.isEnabled()) {
            if (naptanRespository.contains(stationId)) {
                return getAreaFromNaptanData(stationId);
            } else {
                logger.warn("No naptan data found for " + stationId);
            }
        }
        return "";
    }

    @Override
    public IdFor<Station> formStationId(String text) {
        return getStationIdFor(text);
    }

    @NotNull
    public static IdFor<Station> getStationIdFor(String text) {
        if (text.startsWith(METROLINK_ID_PREFIX)) {
            // metrolink platform ids include platform as final digit, remove to give id of station itself
            int index = text.length()-1;
            return StringIdFor.createId(text.substring(0,index));
        }
        return StringIdFor.createId(text);
    }

    private boolean isMetrolinkTram(StopData stopData) {
        return stopData.getId().startsWith(METROLINK_ID_PREFIX);
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

    // TODO Consolidate handling of various TFGM mappings and monitor if still needed
    // spelt different ways within data
    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    @Override
    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        GTFSTransportationType routeType = routeData.getRouteType();
        boolean isMetrolink = Agency.IsMetrolink(agencyId);

        // NOTE: this data issue has been reported to TFGM
        if (isMetrolink && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            return GTFSTransportationType.tram;
        }

        // NOTE: this data issue has been reported to TFGM
        if ( (routeType==GTFSTransportationType.tram) && (!isMetrolink) ) {
            logger.error("Tram transport type seen for non-metrolink agency for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.bus.name() + " for " + routeData);
            return GTFSTransportationType.bus;
        }

        return routeType;

    }

}
