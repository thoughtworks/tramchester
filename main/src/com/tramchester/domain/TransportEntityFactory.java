package com.tramchester.domain;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
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
    private final TramchesterConfig config;

    public TransportEntityFactory(TramchesterConfig config) {
        this.config = config;
    }

    public Agency createAgency(AgencyData agencyData) {
        return new Agency(agencyData.getId(), agencyData.getName());
    }

    public Route createRoute(GTFSTransportationType routeType, RouteData routeData, Agency agency) {
        IdFor<Route> routeId = routeData.getId();


        String routeName = routeData.getLongName();
        if (config.getRemoveRouteNameSuffix()) {
            int indexOf = routeName.indexOf("(");
            if (indexOf > -1) {
                routeName = routeName.substring(0,indexOf).trim();
            }
        }

        return new Route(routeId, routeData.getShortName().trim(), routeName, agency,
                TransportMode.fromGTFS(routeType), routeData.getRouteDirection());

    }

    public Service createService(IdFor<Service> serviceId, Route route) {
        return new Service(serviceId, route);
    }

    public Trip createTrip(TripData tripData, Service service, Route route) {
        return new Trip(tripData.getTripId(), tripData.getHeadsign(), service, route);
    }

    public Station createStation(IdFor<Station> stationId, StopData stopData, GridPosition position) {

        return new Station(stationId, stopData.getArea(), workAroundName(stopData.getName()),
                stopData.getLatLong(), position);
    }

    public Platform createPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName(), stop.getLatLong());
    }


    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
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
}
