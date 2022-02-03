package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRespository;
import com.tramchester.repository.naptan.NaptanStopType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency) {

        IdFor<Route> routeId = createRouteId(routeData.getId());
        String routeName = routeData.getLongName();
        return new MutableRoute(routeId, routeData.getShortName().trim(), routeName, agency, GTFSTransportationType.toTransportMode(routeType));
    }

    @Override
    public MutableStation createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        boolean isInterchange = false;
        IdFor<NaptanArea> areaId = IdFor.invalid();
        if (naptanRespository.isEnabled()) {
            // enrich details from naptan where possible
            if (naptanRespository.containsActo(stationId)) {
                NaptanRecord naptanData = naptanRespository.getForActo(stationId);
                areaId = chooseArea(naptanRespository, naptanData.getAreaCodes());
                isInterchange = NaptanStopType.isInterchance(naptanData.getStopType());
            }
        }

        // NOTE: Tram data has unique positions for each platform
        // todo Can fetch from naptan, which has entires for the stations and platforms
        // Check for duplicate names - handled by CompositeStationRepository

        final String stationName = cleanStationName(stopData);

        return new MutableStation(stationId, areaId, workAroundName(stationName), stopData.getLatLong(), position,
                getDataSourceId(), isInterchange);
    }

    @Override
    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) {
        if (!isMetrolinkTram(stopData)) {
            return Optional.empty();
        }

        String stopId = stopData.getId();
        final IdFor<Platform> platformId = StringIdFor.createId(stopId);

        String platformNumber = stopId.substring(stopId.length()-1);

        IdFor<NaptanArea> areaId = IdFor.invalid();
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(stopData.getLatLong());
        if (naptanRespository.isEnabled()) {
            NaptanRecord naptanData = naptanRespository.getForActo(platformId);
            areaId = chooseArea(naptanRespository, naptanData.getAreaCodes());
            gridPosition = naptanData.getGridPosition(); // often more accurate

            // TODO Add logging if there is a significant diff in position data?
        }

        final MutablePlatform platform = new MutablePlatform(platformId, cleanStationName(stopData),
                getDataSourceId(), platformNumber, areaId, stopData.getLatLong(), gridPosition, station.isMarkedInterchange());
        return Optional.of(platform);

    }

    private String cleanStationName(StopData stopData) {
        String text = stopData.getName();
        text = text.replace("\"", "").trim();

        if (text.endsWith(METROLINK_NAME_POSTFIX)) {
            return text.replace(METROLINK_NAME_POSTFIX,"").trim();
        } else {
            return text;
        }
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

    private String getAreaFromNaptanData(NaptanRecord naptanStopData) {
        String area;
        area = naptanStopData.getSuburb();
        String parent = naptanStopData.getTown();
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
