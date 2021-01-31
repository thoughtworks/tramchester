package com.tramchester.domain.factory;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.NoPlatformStopCall;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

public class TransportEntityFactory {
    protected final TramchesterConfig config;

    public TransportEntityFactory(TramchesterConfig config) {
        this.config = config;
    }

    public Agency createAgency(DataSourceID dataSourceID, AgencyData agencyData) {
        return new Agency(dataSourceID, agencyData.getId(), agencyData.getName());
    }

    public Agency createUnknownAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId) {
        return new Agency(dataSourceID, agencyId.getGraphId(), "UNKNOWN");
    }

    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency, IdMap<Station> allStations) {
        IdFor<Route> routeId = routeData.getId();

        return new Route(routeId, routeData.getShortName().trim(), routeData.getLongName(), agency,
                TransportMode.fromGTFS(routeType), routeData.getRouteDirection());

    }

    public Service createService(IdFor<Service> serviceId, Route route) {
        return new Service(serviceId, route);
    }

    public Trip createTrip(TripData tripData, Service service, Route route) {
        return new Trip(tripData.getTripId(), tripData.getHeadsign(), service, route);
    }

    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        return new Station(stationId, stopData.getArea(), stopData.getName(), stopData.getLatLong(), position);
    }

    public Platform createPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName(), stop.getLatLong());
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
}
