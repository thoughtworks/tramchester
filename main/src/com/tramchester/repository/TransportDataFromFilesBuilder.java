package com.tramchester.repository;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.BusStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataFromFilesBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFilesBuilder.class);

    private final List<TransportDataStreams> transportDataStreams;
    private final StationLocations stationLocations;

    private final Set<String> excludedRoutes;
    private final Set<String> excludedTrips;
    private final Set<String> excludedServices;
    private TransportDataContainer buildable;

    public TransportDataFromFilesBuilder(List<TransportDataStreams> transportDataStreams, StationLocations stationLocations) {
        this.transportDataStreams = transportDataStreams;
        this.stationLocations = stationLocations;
        this.excludedRoutes = new HashSet<>();
        this.excludedTrips = new HashSet<>();
        this.excludedServices = new HashSet<>();
        buildable = null;
    }

    public TransportDataSource getData() {
        return buildable;
    }

    public void load() {
        logger.info("Loading transport data from files");

        buildable = new TransportDataContainer();

        transportDataStreams.forEach(this::load);

        logger.info("Finished loading transport data");
    }

    private void load(TransportDataStreams streams) {
        if(streams.hasFeedInfo()) {
            FeedInfo feedInfo = streams.feedInfo.findFirst().get();
            buildable.SetFeedInfo(feedInfo);
            buildable.SetVersion(feedInfo.getVersion());
        } else {
            // TODO Base on file mod time??
            buildable.SetVersion(UUID.randomUUID().toString());
            logger.warn("Do no have feedinfo for this data source");
        }

        populateAgencies(buildable, streams.agencies);
        populateStationsAndAreas(buildable, streams.stops);
        populateRoutes(buildable, streams.routes);
        populateTrips(buildable, streams.trips);
        populateStopTimes(buildable, streams.stopTimes);
        populateCalendars(buildable, streams.calendars, streams.calendarsDates);
        buildable.updateTimesForServices();

        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("Service %s has missing date data or runs on zero days", svc.getId()))
        );
        streams.closeAll();
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars, Stream<CalendarDateData> calendarsDates) {

        Set<String> missingCalendar = new HashSet<>();
        calendars.forEach(calendar -> {
            String serviceId = calendar.getServiceId();
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

        Set<String> missingCalendarDates = new HashSet<>();
        calendarsDates.forEach(date -> {
            String serviceId = date.getServiceId();
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

    private void populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes) {
        logger.info("Loading stop times");
        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> !excludedTrips.contains(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            Trip trip = buildable.getTrip(stopTimeData.getTripId());
            String platformId = stopTimeData.getStopId();
            String stationId = Station.formId(platformId);

            if (buildable.hasStationId(stationId)) {
                Route route = trip.getRoute();
                Station station =  buildable.getStation(stationId);
                station.addRoute(route);
                RouteStation routeStation = new RouteStation(station, route);
                if (!buildable.hasRouteStation(routeStation.getId())) {
                    buildable.addRouteStation(routeStation);
                }
                byte stopSequence = Byte.parseByte(stopTimeData.getStopSequence());

                StopCall stop;
                if (route.isTram()) {
                    if (buildable.hasPlatformId(platformId)) {
                        Platform platform = buildable.getPlatform(platformId);
                        platform.addRoute(route);
                    } else {
                        logger.error("Missing platform " +platformId);
                    }
                    Platform platform = buildable.getPlatform(platformId);
                    stop = new TramStopCall(platform, station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                } else
                {
                    stop = new BusStopCall(station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                }
                count.getAndIncrement();

                trip.addStop(stop);
            } else {
                logger.warn(format("Cannot find station for Id '%s' for stopId '%s'", stationId, platformId));
            }
        });
        logger.info("Loaded " + count.get() + " stop times");
    }

    private void populateTrips(TransportDataContainer buildable, Stream<TripData> trips) {
        logger.info("Loading trips");
        AtomicInteger count = new AtomicInteger();

        trips.forEach((tripData) -> {
            String serviceId = tripData.getServiceId();
            String routeId = tripData.getRouteId();
            Route route = buildable.getRoute(routeId);

            if (route != null) {
                Service service = getOrInsertService(buildable, serviceId, route);
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
    }

    private void populateAgencies(TransportDataContainer buildable, Stream<AgencyData> agencyDataStream) {
        logger.info("Loading agencies");
        agencyDataStream.forEach(agencyData -> buildable.addAgency(new Agency(agencyData.getId(), agencyData.getName())));
    }

    private void populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream) {
        logger.info("Loading routes");
        routeDataStream.forEach(routeData -> {
            String agencyId = routeData.getAgency();
            if (!buildable.hasAgencyId(agencyId)) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = GTFSTransportationType.getType(routeData.getRouteType());

            if (GTFSTransportationType.supportedType(routeType)) {
                Agency agency = buildable.getAgency(agencyId);
                Route route = new Route(routeData.getId(), routeData.getShortName().trim(), routeData.getLongName(), agency,
                        TransportMode.fromGTFS(routeType));
                buildable.addRoute(route);
                buildable.addRouteToAgency(agency, route);
            } else {
                excludedRoutes.add(routeData.getId());
                logger.info("Unsupported GTFS transport type: " + routeType + " agency:" + routeData.getAgency() + " routeId: " + routeData.getId());
            }
        });
    }

    private void populateStationsAndAreas(TransportDataContainer buildable, Stream<StopData> stops) {
        logger.info("Loading stops");
        stops.forEach((stop) -> {
            String stopId = stop.getId();
            Station station;
            String stationId = Station.formId(stopId);

            if (!buildable.hasStationId(stationId)) {
                station = new Station(stationId, stop.getArea(), stop.getName(), stop.getLatLong(), stop.isTram());
                buildable.addStation(station);
                stationLocations.addStation(station);
            } else {
                station = buildable.getStation(stationId);
            }

            // TODO Trains?
            if (stop.isTram()) {
                Platform platform;
                if (!buildable.hasPlatformId(stopId)) {
                    platform = formPlatform(stop);
                    buildable.addPlatform(platform);
                } else {
                    platform = buildable.getPlatform(stopId);
                }
                if (!station.getPlatforms().contains(platform)) {
                    station.addPlatform(platform);
                }
            }
        });
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName());
    }



    private Service getOrInsertService(TransportDataContainer buildable, String serviceId, Route route) {
        if (!buildable.hasServiceId(serviceId)) {
            buildable.addService(new Service(serviceId, route));
            excludedServices.remove(serviceId);
        }
        Service service = buildable.getService(serviceId);
        service.addRoute(route);
        return service;
    }

    private Trip getOrCreateTrip(TransportDataContainer buildable, String tripId, String tripHeadsign, Service service, Route route) {
        if (buildable.hasTripId(tripId)) {
            Trip matched = buildable.getTrip(tripId);
            if ((!matched.getRoute().equals(route)) || !matched.getService().equals(service) || !matched.getHeadsign().equals(tripHeadsign)) {
                logger.error("Mismatch for trip id: " + tripId + " (mis)matched was " + matched);
            }
            return matched;
        }

        Trip trip = new Trip(tripId, tripHeadsign, service, route);
        buildable.addTrip(trip);
        return trip;
    }

    public static class TransportDataStreams {
        final Stream<StopData> stops;
        final Stream<RouteData> routes;
        final Stream<TripData> trips;
        final Stream<StopTimeData> stopTimes;
        final Stream<CalendarData> calendars;
        final Stream<FeedInfo> feedInfo;
        final Stream<CalendarDateData> calendarsDates;
        final Stream<AgencyData> agencies;
        final private boolean expectFeedInfo;

        public TransportDataStreams(Stream<AgencyData> agencies, Stream<StopData> stops, Stream<RouteData> routes, Stream<TripData> trips,
                                    Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                    Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates, boolean expectFeedInfo) {
            this.agencies = agencies;
            this.stops = stops;
            this.routes = routes;
            this.trips = trips;
            this.stopTimes = stopTimes;
            this.calendars = calendars;
            this.feedInfo = feedInfo;
            this.calendarsDates = calendarsDates;
            this.expectFeedInfo = expectFeedInfo;
        }

        public void closeAll() {
            stops.close();
            routes.close();
            trips.close();
            stopTimes.close();
            calendars.close();
            feedInfo.close();
            calendarsDates.close();
            agencies.close();
        }

        public boolean hasFeedInfo() {
            return expectFeedInfo;
        }
    }

}
