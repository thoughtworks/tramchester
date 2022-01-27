package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRespository;

import java.util.Optional;

public abstract class TransportEntityFactory {

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

    public MutableStation createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {
        final String area = "";

        IdFor<NaptanArea> areaId = IdFor.invalid();
        return new MutableStation(stationId, areaId, stopData.getName(), stopData.getLatLong(), position, getDataSourceId());
    }

    public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    public StopCall createPlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        return new PlatformStopCall(platform, station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
            stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
        //return new PlatformStopCall(trip, platform, station, stopTimeData);
    }

    public StopCall createNoPlatformStopCall(Trip trip, Station station, StopTimeData stopTimeData) {
        return new NoPlatformStopCall(station, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime(),
                stopTimeData.getStopSequence(), stopTimeData.getPickupType(), stopTimeData.getDropOffType(), trip);
        //return new NoPlatformStopCall(trip, station, stopTimeData);
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

    public Optional<MutablePlatform> maybeCreatePlatform(StopData stopData) {
        return Optional.empty();
    }

    public static IdFor<NaptanArea> chooseArea(NaptanRespository naptanRespository, IdSet<NaptanArea> areaCodes) {
        if (areaCodes.isEmpty()) {
            return IdFor.invalid();
        }

        IdSet<NaptanArea> active = naptanRespository.activeCodes(areaCodes);
        if (active.size()==1) {
            return active.toList().get(0);
        }
        // TODO this seems not to happen, while StopPoints have more that one area it always seemed be marked
        // inactive in the StopPoint or the StopArea
        throw new RuntimeException("Todo, need to check if more than one active code is present in the data set " + areaCodes);
    }
}
