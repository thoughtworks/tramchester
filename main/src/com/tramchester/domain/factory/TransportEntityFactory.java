package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

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
                TransportMode.fromGTFS(routeType));

    }

    public MutableService createService(IdFor<Service> serviceId) {
        return new MutableService(serviceId);
    }

    public MutableTrip createTrip(TripData tripData, Service service, Route route) {
        return new MutableTrip(tripData.getTripId(), tripData.getHeadsign(), service, route);
    }

    public MutableStation createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        final String area = "";

        return new MutableStation(stationId, area, stopData.getName(), stopData.getLatLong(), position, getDataSourceId());
    }

    public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    public StopCall createPlatformStopCall(Trip trip, Platform platform, Station station, StopTimeData stopTimeData) {
        return new PlatformStopCall(trip, platform, station, stopTimeData);
    }

    public StopCall createNoPlatformStopCall(Trip trip, Station station, StopTimeData stopTimeData) {
        return new NoPlatformStopCall(trip, station, stopTimeData);
    }

    public ServiceCalendar createServiceCalendar(CalendarData calendarData) {
        return new ServiceCalendar(calendarData);
    }

    public GTFSTransportationType getRouteType(RouteData routeData, IdFor<Agency> agencyId) {
        return routeData.getRouteType();
    }

    public IdFor<Route> createRouteId(IdFor<Route> routeId) {
        return routeId;
    }

    public abstract IdFor<Station> formStationId(String stopId);

    public abstract void updateStation(MutableStation station, StopData stopData);
}
