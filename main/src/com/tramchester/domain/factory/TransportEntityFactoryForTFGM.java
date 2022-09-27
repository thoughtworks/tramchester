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
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.naptan.NaptanStopType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static java.lang.String.format;

public class TransportEntityFactoryForTFGM extends TransportEntityFactory {

    private static final String METROLINK_ID_PREFIX = "9400ZZ";
    private static final String METROLINK_NAME_POSTFIX = "(Manchester Metrolink)";

    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactoryForTFGM.class);

    private final NaptanRepository naptanRespository;

    private final Duration minChangeDuration = Duration.ofMinutes(MutableStation.DEFAULT_MIN_CHANGE_TIME);

    public TransportEntityFactoryForTFGM(NaptanRepository naptanRespository) {
        super();
        this.naptanRespository = naptanRespository;
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.tfgm;
    }

    @Override
    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency) {

        IdFor<Route> routeId = RouteIdSwapWorkaround.getCorrectIdFor(routeData);

        String routeName = routeData.getLongName();
        return new MutableRoute(routeId, routeData.getShortName().trim(), routeName, agency,
                GTFSTransportationType.toTransportMode(routeType));
    }

    @Override
    public MutableStation createStation(IdFor<Station> stationId, StopData stopData) {

        boolean isInterchange = false;
        IdFor<NaptanArea> areaId = IdFor.invalid();
        LatLong latLong = stopData.getLatLong();
        GridPosition position = CoordinateTransforms.getGridPosition(latLong);

        if (naptanRespository.isEnabled()) {
            // enrich details from naptan where possible
            if (naptanRespository.containsActo(stationId)) {
                NaptanRecord naptanData = naptanRespository.getForActo(stationId);

                isInterchange = NaptanStopType.isInterchance(naptanData.getStopType());
                areaId = chooseArea(naptanRespository, naptanData.getAreaCodes());
                position = naptanData.getGridPosition();
                latLong = naptanData.getLatLong();
            }
        }

        final String stationName = cleanStationName(stopData);

        return new MutableStation(stationId, areaId, workAroundName(stationName), latLong, position,
                getDataSourceId(), isInterchange, minChangeDuration);
    }

    @Override
    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) {
        if (!isMetrolinkTram(stopData)) {
            return Optional.empty();
        }

        final String stopId = stopData.getId();
        final IdFor<Platform> platformId = StringIdFor.createId(stopId);

        final String platformNumber = stopId.substring(stopId.length()-1);

        IdFor<NaptanArea> areaId = IdFor.invalid();
        LatLong latLong = stopData.getLatLong();
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);

        if (naptanRespository.isEnabled()) {
            NaptanRecord naptanData = naptanRespository.getForActo(platformId);

            areaId = chooseArea(naptanRespository, naptanData.getAreaCodes());
            gridPosition = naptanData.getGridPosition();
            latLong = naptanData.getLatLong();

            // TODO Add logging if there is a significant diff in position data?
        }

        final MutablePlatform platform = new MutablePlatform(platformId, station, cleanStationName(stopData),
                getDataSourceId(), platformNumber, areaId, latLong, gridPosition, station.isMarkedInterchange());
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

    private static class RouteIdSwapWorkaround {
        private final String idPrefixFromData;
        private final String replacementPrefix;
        private final String mustMatchLongName;

        private static final Map<String, RouteIdSwapWorkaround> mapping;

        static {
            List<RouteIdSwapWorkaround> table = Arrays.asList(
                    of("METLRED:I:", "METLRED:O:", "Cornbrook - The Trafford Centre"),
                    of("METLRED:O:", "METLRED:I:", "The Trafford Centre - Cornbrook"),
                    of("METLNAVY:I:", "METLNAVY:O:", "Victoria - Wythenshawe - Manchester Airport"),
                    of("METLNAVY:O:", "METLNAVY:I:", "Manchester Airport - Wythenshawe - Victoria"));
            mapping = new HashMap<>();
            table.forEach(item -> mapping.put(item.mustMatchLongName, item));
        }

        private static RouteIdSwapWorkaround of(String idPrefixFromData, String replacementPrefix, String mustMatchLongName) {
            return new RouteIdSwapWorkaround(idPrefixFromData, replacementPrefix, mustMatchLongName);
        }

        private RouteIdSwapWorkaround(String idPrefixFromData, String replacementPrefix, String mustMatchLongName) {
            this.idPrefixFromData = idPrefixFromData;
            this.replacementPrefix = replacementPrefix;
            this.mustMatchLongName = mustMatchLongName;
        }

        public static IdFor<Route> getCorrectIdFor(RouteData routeData) {
            String longName = routeData.getLongName();
            if (!mapping.containsKey(longName)) {
                return routeData.getId();
            }

            String idAsString = routeData.getId().forDTO();
            RouteIdSwapWorkaround workaroundForLongname = mapping.get(longName);
            if (idAsString.startsWith(workaroundForLongname.idPrefixFromData)) {
                String replacementIdAsString = idAsString.replace(workaroundForLongname.idPrefixFromData, workaroundForLongname.replacementPrefix);
                logger.warn(format("Workaround for route ID issue, replaced %s with %s", idAsString, replacementIdAsString));
                return StringIdFor.createId(replacementIdAsString);
            } else {
                logger.warn("Workaround for " + routeData + " no longer needed?");
                return routeData.getId();
            }

        }
    }

}
