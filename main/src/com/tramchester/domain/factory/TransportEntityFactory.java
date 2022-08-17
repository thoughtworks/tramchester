package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.lang.String.format;

public abstract class TransportEntityFactory {

    private static final Logger logger = LoggerFactory.getLogger(TransportEntityFactory.class);

    public TransportEntityFactory() {
    }

    public abstract DataSourceID getDataSourceId();

    public MutableAgency createAgency(DataSourceID dataSourceID, AgencyData agencyData) {
        return new MutableAgency(dataSourceID, agencyData.getId(), agencyData.getName());
    }

    public MutableAgency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId) {
        return new MutableAgency(dataSourceID, agencyId, "UNKNOWN");
    }

    public MutableRoute createRoute(GTFSTransportationType routeType, RouteData routeData, MutableAgency agency) {

        IdFor<Route> routeId = routeData.getId();

        return new MutableRoute(routeId, routeData.getShortName().trim(), routeData.getLongName(), agency,
                GTFSTransportationType.toTransportMode(routeType));

    }

    public MutableService createService(IdFor<Service> serviceId) {
        return new MutableService(serviceId);
    }

    public MutableTrip createTrip(TripData tripData, MutableService service, Route route, TransportMode transportMode) {
        final MutableTrip trip = new MutableTrip(tripData.getTripId(), tripData.getHeadsign(), service, route, transportMode);
        service.addTrip(trip);
        return trip;
    }

    public MutableStation createStation(IdFor<Station> stationId, StopData stopData) {

        IdFor<NaptanArea> areaId = IdFor.invalid();
        GridPosition position = CoordinateTransforms.getGridPosition(stopData.getLatLong());
        return new MutableStation(stationId, areaId, stopData.getName(), stopData.getLatLong(), position, getDataSourceId());
    }

    public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    public StopCall createPlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        return new PlatformStopCall(platform, station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
            stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    public StopCall createNoPlatformStopCall(Trip trip, Station station, StopTimeData stopTimeData) {
        return new NoPlatformStopCall(station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
                stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
    }

    public MutableServiceCalendar createServiceCalendar(CalendarData calendarData) {
        return new MutableServiceCalendar(calendarData);
    }

    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        return routeData.getRouteType();
    }

    public IdFor<Route> createRouteId(IdFor<Route> routeId) {
        return routeId;
    }

    public abstract IdFor<Station> formStationId(String stopId);

    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData, Station station) {
        return Optional.empty();
    }

    public static IdFor<NaptanArea> chooseArea(NaptanRepository naptanRespository, IdSet<NaptanArea> areaCodes) {
        if (areaCodes.isEmpty()) {
            return IdFor.invalid();
        }

        IdSet<NaptanArea> active = naptanRespository.activeCodes(areaCodes);
        if (active.isEmpty()) {
            logger.info(format("None of the area codes %s were active ", areaCodes));
            return IdFor.invalid();
        }
        if (active.size()==1) {
            return active.toList().get(0);
        }

        final String message = "More than one active code is present in the data set " + areaCodes;
        logger.error(message);
        throw new RuntimeException(message);
    }
}
