package com.tramchester.domain.factory;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.input.NoPlatformStopCall;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

public abstract class TransportEntityFactory {

    public TransportEntityFactory() {
    }

    public abstract DataSourceID getDataSourceId();

    public Agency createAgency(DataSourceID dataSourceID, AgencyData agencyData) {
        return new Agency(dataSourceID, agencyData.getId(), agencyData.getName());
    }

    public Agency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId) {
        return new Agency(dataSourceID, agencyId, "UNKNOWN");
    }

    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {
        IdFor<Route> routeId = routeData.getId();

        return new Route(routeId, routeData.getShortName().trim(), routeData.getLongName(), agency,
                TransportMode.fromGTFS(routeType));

    }

    public Service createService(IdFor<Service> serviceId) {
        return new Service(serviceId);
    }

    public Trip createTrip(TripData tripData, Service service, Route route) {
        return new Trip(tripData.getTripId(), tripData.getHeadsign(), service, route);
    }

    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        final String area = "";

        return new Station(stationId, area, stopData.getName(), stopData.getLatLong(), position, getDataSourceId());
    }

    public RouteStation createRouteStation(Station station, Route route) {
        return new RouteStation(station, route);
    }

    public StopCall createPlatformStopCall(Platform platform, Station station, StopTimeData stopTimeData) {
        return new PlatformStopCall(platform, station, stopTimeData);
    }

    public StopCall createNoPlatformStopCall(Station station, StopTimeData stopTimeData) {
        return new NoPlatformStopCall(station, stopTimeData);
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

    public abstract void updateStation(Station station, StopData stopData);
}
