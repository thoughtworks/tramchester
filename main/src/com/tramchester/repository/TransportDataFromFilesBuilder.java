package com.tramchester.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataFromFilesBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFilesBuilder.class);

    private final List<TransportDataStreams> transportDataStreams;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    private TransportDataContainer toBuild;

    public TransportDataFromFilesBuilder(List<TransportDataStreams> transportDataStreams, StationLocations stationLocations,
                                         TramchesterConfig config) {
        this.transportDataStreams = transportDataStreams;
        this.stationLocations = stationLocations;
        this.config = config;
        toBuild = null;
    }

    public TransportData getData() {
        return toBuild;
    }

    public void load() {
        toBuild = new TransportDataContainer();
        logger.info("Loading transport data from files");
        transportDataStreams.forEach(transportDataStream -> load(transportDataStream, toBuild));
        logger.info("Finished loading transport data");
    }

    private void load(TransportDataStreams streams, TransportDataContainer buildable) {
        String sourceName = streams.getNameAndVersion().getName();
        DataSourceConfig sourceConfig = streams.getConfig();
        logger.info("Loading data for " + sourceName);
        DataSourceInfo.NameAndVersion fromStreams = streams.getNameAndVersion();
        if(sourceConfig.getHasFeedInfo()) {
            FeedInfo feedInfo = streams.feedInfo.findFirst().get();
            String name = fromStreams.getName();
            buildable.addNameAndVersion(new DataSourceInfo.NameAndVersion(name, feedInfo.getVersion()));
            buildable.addFeedInfo(name, feedInfo);
        } else {
            logger.warn("No feedinfo for " + sourceName);
            buildable.addNameAndVersion(fromStreams);
        }

        IdMap<Agency> allAgencies = preloadAgencys(streams.agencies);
        IdSet<Route> excludedRoutes = populateRoutes(buildable, streams.routes, allAgencies, sourceConfig);
        allAgencies.clear();

        ExcludedTripAndServices excludedTripsAndServices = populateTripsAndServices(buildable, streams.trips, excludedRoutes);
        excludedRoutes.clear();

        IdMap<Station> allStations = preLoadStations(streams.stops);
        populateStopTimes(buildable, streams.stopTimes, allStations, excludedTripsAndServices.excludedTrips);
        allStations.clear();

        populateCalendars(buildable, streams.calendars, streams.calendarsDates, excludedTripsAndServices.excludedServices);
        excludedTripsAndServices.clear();

        buildable.updateTimesForServices();
        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("source %s Service %s has missing date data or runs on zero days",
                        sourceName, svc.getId()))
        );
        streams.closeAll();
        logger.info("Finishing Loading data for " + sourceName);
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdSet<Service> excludedServices) {

        logger.info("Loading calendars");
        IdSet<Service> missingCalendar = new IdSet<>();
        calendars.forEach(calendar -> {
            IdFor<Service> serviceId = calendar.getServiceId();
            Service service = buildable.getService(serviceId);

            if (service != null) {
                service.setDays(
                        calendar.isMonday(),
                        calendar.isTuesday(),
                        calendar.isWednesday(),
                        calendar.isThursday(),
                        calendar.isFriday(),
                        calendar.isSaturday(),
                        calendar.isSunday()
                );
                service.setServiceDateRange(calendar.getStartDate(), calendar.getEndDate());
            } else {
                if (!excludedServices.contains(serviceId)) {
                    missingCalendar.add(serviceId);
                }
            }
        });
        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar.toString() + " for calendar");
        }

        logger.info("Loading calendar dates");
        IdSet<Service> missingCalendarDates = new IdSet<>();
        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            Service service = buildable.getService(serviceId);
            if (service != null) {
                service.addExceptionDate(date.getDate(), date.getExceptionType());
            } else {
                if (!excludedServices.contains(serviceId)) {
                    missingCalendarDates.add(serviceId);
                }
            }
        });
        if (!missingCalendarDates.isEmpty()) {
            logger.warn("Failed to find service id " + missingCalendarDates.toString() + " for calendar_dates");
        }
    }

    private void populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes,
                                   IdMap<Station> allStations, IdSet<Trip> excludedTrips) {
        logger.info("Loading stop times");
        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> !excludedTrips.contains(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            Trip trip = buildable.getTripById(stopTimeData.getTripId());
            String stopId = stopTimeData.getStopId();
            IdFor<Station> stationId = Station.formId(stopId);

            if (allStations.hasId(stationId)) {
                Route route = trip.getRoute();
                Station station = allStations.get(stationId);
                addStation(buildable, route, station);

                addStop(buildable, stopTimeData, trip, stopId, route, station);

                count.getAndIncrement();

            } else {
                logger.warn(format("Cannot find station for Id '%s' for stopId '%s'", stationId, stopId));
            }
        });
        logger.info("Loaded " + count.get() + " stop times");
    }

    private void addStop(TransportDataContainer buildable, StopTimeData stopTimeData, Trip trip,
                         String stopIdText, Route route, Station station) {
        StopCall stop;
        switch (route.getTransportMode()) {
            case Tram:
                IdFor<Platform> stopId = IdFor.createId(stopIdText);
                if (buildable.hasPlatformId(stopId)) {
                    Platform platform = buildable.getPlatform(stopId);
                    platform.addRoute(route);
                } else {
                    logger.error("Missing platform " + stopId);
                }
                Platform platform = buildable.getPlatform(stopId);
                stop = new TramStopCall(platform, station, stopTimeData);
                break;
            case Bus:
                stop = new BusStopCall(station, stopTimeData);
                break;
            case Train:
                stop = new TrainStopCall(station, stopTimeData);
                break;
            default:
                throw new RuntimeException("Unexpected transport mode " + route.getTransportMode());

        }

        trip.addStop(stop);
    }

    private void addStation(TransportDataContainer buildable, Route route, Station station) {
        stationLocations.addStation(station);
        station.addRoute(route);

        IdFor<Station> stationId = station.getId();
        if (!buildable.hasStationId(stationId)) {
            buildable.addStation(station);
            if (station.hasPlatforms()) {
                station.getPlatforms().forEach(buildable::addPlatform);
            }
        }
        RouteStation routeStation = new RouteStation(station, route);
        if (!buildable.hasRouteStationId(routeStation.getId())) {
            buildable.addRouteStation(routeStation);
        }
    }

    private ExcludedTripAndServices populateTripsAndServices(TransportDataContainer buildable, Stream<TripData> trips,
                                          IdSet<Route> excludedRoutes) {
        logger.info("Loading trips");
        IdSet<Trip> excludedTrips = new IdSet<>();
        IdSet<Service> excludedServices = new IdSet<>();
        AtomicInteger count = new AtomicInteger();

        trips.forEach((tripData) -> {
            IdFor<Service> serviceId = tripData.getServiceId();
            IdFor<Route> routeId = tripData.getRouteId();

            if (buildable.hasRouteId(routeId)) {
                Route route = buildable.getRouteById(routeId);

                Service service = getOrInsertService(buildable, serviceId, route, excludedServices);
                Trip trip = getOrCreateTrip(buildable, tripData.getTripId(), tripData.getTripHeadsign(), service, route );
                count.getAndIncrement();
                service.addTrip(trip);
                route.addService(service);
                route.addHeadsign(trip.getHeadsign());
            } else {
                if (excludedRoutes.contains(routeId)) {
                    excludedTrips.add(tripData.getTripId());
                    if (!buildable.hasServiceId(serviceId)) {
                        excludedServices.add(serviceId);
                    }
                } else {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get());
        return new ExcludedTripAndServices(excludedTrips, excludedServices);
    }

    private  IdMap<Agency> preloadAgencys(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading all agencies");
        IdMap<Agency> agencies = new IdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(new Agency(agencyData.getId(), agencyData.getName())));
        logger.info("Loaded " + agencies.size() + " agencies");
        return agencies;
    }

    private IdSet<Route> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream,
                                       IdMap<Agency> allAgencies, DataSourceConfig sourceConfig) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportModes();
        AtomicInteger count = new AtomicInteger();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        IdSet<Route> excludedRoutes = new IdSet<>();
        routeDataStream.forEach(routeData -> {
            IdFor<Agency> agencyId = routeData.getAgencyId();
            if (!allAgencies.hasId(agencyId)) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = routeData.getRouteType();

            IdFor<Route> id = routeData.getId();
            if (transportModes.contains(routeType)) {
                String routeName = routeData.getLongName();
                if (config.getRemoveRouteNameSuffix()) {
                    int indexOf = routeName.indexOf("(");
                    if (indexOf > -1) {
                        routeName = routeName.substring(0,indexOf).trim();
                    }
                }

                count.getAndIncrement();
                Agency agency = allAgencies.get(agencyId);
                Route route = new Route(id, routeData.getShortName().trim(), routeName, agency,
                        TransportMode.fromGTFS(routeType));
                buildable.addAgency(agency);
                buildable.addRoute(route);
                buildable.addRouteToAgency(agency, route);
            } else {
                excludedRoutes.add(id);
            }
        });
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.size());
        return excludedRoutes;
    }

    private IdMap<Station> preLoadStations(Stream<StopData> stops) {
        logger.info("Loading all stops");
        IdMap<Station> allStations = new IdMap<>();

        stops.forEach((stop) -> {

            String stopId = stop.getId();
            IdFor<Station> stationId = Station.formId(stopId);

            Station station = allStations.getOrAdd(stationId, () ->
                    new Station(stationId, stop.getArea(), workAroundName(stop.getName()), stop.getLatLong()));

//            if (!allStations.containsKey(stationId)) {
//                station = new Station(stationId, stop.getArea(), workAroundName(stop.getName()), stop.getLatLong());
//                allStations.put(station.getId(), station);
//            } else {
//                station = allStations.get(stationId);
//            }

            // TODO Trains?
            if (stop.isTFGMTram()) {
//                Platform platform;
//                if (!buildable.hasPlatformId(stopId)) {
//                    buildable.addPlatform(platform);
//                } else {
//                    platform = buildable.getPlatform(stopId);
//                }

                Platform platform = formPlatform(stop);
                if (!station.getPlatforms().contains(platform)) {
                    station.addPlatform(platform);
                }
            }

        });
        logger.info("Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName());
    }

    private Service getOrInsertService(TransportDataContainer buildable, IdFor<Service> serviceId, Route route,
                                       IdSet<Service> excludedServices) {
        if (!buildable.hasServiceId(serviceId)) {
            buildable.addService(new Service(serviceId, route));
            excludedServices.remove(serviceId);
        }
        Service service = buildable.getService(serviceId);
        service.addRoute(route);
        return service;
    }

    private Trip getOrCreateTrip(TransportDataContainer buildable, IdFor<Trip> tripId, String tripHeadsign, Service service, Route route) {
        if (buildable.hasTripId(tripId)) {
            Trip matched = buildable.getTripById(tripId);
            if ((!matched.getRoute().equals(route)) || !matched.getService().equals(service) || !matched.getHeadsign().equals(tripHeadsign)) {
                logger.error("Mismatch for trip id: " + tripId + " (mis)matched was " + matched);
            }
            return matched;
        }

        Trip trip = new Trip(tripId, tripHeadsign, service, route);
        buildable.addTrip(trip);
        return trip;
    }

    private static class ExcludedTripAndServices {
        private final IdSet<Trip> excludedTrips;
        private final IdSet<Service> excludedServices;

        public ExcludedTripAndServices(IdSet<Trip> excludedTrips, IdSet<Service> excludedServices) {
            this.excludedTrips = excludedTrips;
            this.excludedServices = excludedServices;
        }

        public void clear() {
            excludedServices.clear();
            excludedTrips.clear();
        }
    }
}
