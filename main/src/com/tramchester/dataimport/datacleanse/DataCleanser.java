package com.tramchester.dataimport.datacleanse;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.ErrorCount;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.StopDataParser;
import com.tramchester.domain.FeedInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


public class DataCleanser {
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("YYYMMdd");
    private static final String WILDCARD = "*";

    private final TransportDataReaderFactory dataReaderFactory;
    private final TransportDataWriterFactory transportDataWriterFactory;
    private ErrorCount count;
    private final TramchesterConfig config;

    public DataCleanser(TransportDataReaderFactory readerFactory, TransportDataWriterFactory writerFactort,
                        TramchesterConfig config) {
        this.dataReaderFactory = readerFactory;
        this.transportDataWriterFactory = writerFactort;
        this.config = config;
    }

    public ErrorCount run(Set<String> agencies) throws IOException {
        this.count = new ErrorCount();

        List<String> routeCodes = cleanseRoutes(agencies);

        ServicesAndTrips servicesAndTrips = cleanseTrips(routeCodes);

        Set<String> stopIds = cleanseStoptimes(servicesAndTrips.getTripIds());

        cleanseStops(stopIds);

        cleanseCalendar(servicesAndTrips.getServiceIds());

        cleanFeedInfo();

        if (!count.noErrors()) {
            logger.warn("Unable to cleanse all data" + count);
        }
        return count;
    }

    public void cleanseCalendar(Set<String> services) throws IOException {
        logger.info("**** Start cleansing calendar.");

        Stream<CalendarData> calendar = dataReaderFactory.getForCleanser().getCalendar();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("calendar");
        calendar.filter(calendarData -> services.contains(calendarData.getServiceId()) ) //&& calendarData.runsAtLeastADay())
                .forEach(calendarData -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        calendarData.getServiceId(),
                        runsOnDay(calendarData.isMonday()),
                        runsOnDay(calendarData.isTuesday()),
                        runsOnDay(calendarData.isWednesday()),
                        runsOnDay(calendarData.isThursday()),
                        runsOnDay(calendarData.isFriday()),
                        runsOnDay(calendarData.isSaturday()),
                        runsOnDay(calendarData.isSunday()),
                        calendarData.getStartDate().format(DATE_FORMAT),
                        calendarData.getEndDate().format(DATE_FORMAT))));

        writer.close();
        calendar.close();
        logger.info("**** End cleansing calendar.\n\n");
    }

    public Set<String> cleanseStoptimes(Set<String> tripIds) throws IOException {
        logger.info("**** Start cleansing stop times.");
        Set<String> stopIds = new HashSet<>();

        Stream<StopTimeData> stopTimes = dataReaderFactory.getForCleanser().getStopTimes();
        TransportDataWriter writer = transportDataWriterFactory.getWriter("stop_times");

        stopTimes.filter(stopTime -> tripIds.contains(stopTime.getTripId()))
                .forEach(stopTime -> {
                    if (stopTime.isInError()) {
                        logger.warn("Unable to process " + stopTime);
                        count.inc();
                    } else {
                        try {
                            writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                                    stopTime.getTripId(),
                                    stopTime.getArrivalTime().tramDataFormat(),
                                    stopTime.getDepartureTime().tramDataFormat(),
                                    stopTime.getStopId(),
                                    stopTime.getStopSequence(),
                                    stopTime.getPickupType(),
                                    stopTime.getDropOffType()));
                            stopIds.add(stopTime.getStopId());
                        } catch (NullPointerException exception) {
                            count.inc();
                            logger.error("Unable to add " + stopTime, exception);
                        }
                    }
                });

        writer.close();
        stopTimes.close();
        logger.info("**** End cleansing stop times.\n");
        return stopIds;
    }

    public ServicesAndTrips cleanseTrips(List<String> routeCodes) throws IOException {
        logger.info("**** Start cleansing trips.");
        Set<String> uniqueSvcIds = new HashSet<>();
        Set<String> tripIds = new HashSet<>();
        Stream<TripData> trips = dataReaderFactory.getForCleanser().getTrips();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("trips");

        trips.filter(trip -> routeCodes.contains(trip.getRouteId())).forEach(trip -> {
            writer.writeLine(String.format("%s,%s,%s,%s",
                    trip.getRouteId(),
                    trip.getServiceId(),
                    trip.getTripId(),
                    trip.getTripHeadsign()));
            tripIds.add(trip.getTripId());
            uniqueSvcIds.add(trip.getServiceId());
        });
        writer.close();
        trips.close();
        logger.info("**** End cleansing trips.\n\n");
        return new ServicesAndTrips(uniqueSvcIds, tripIds);
    }

    public void cleanseStops(Set<String> stopIds) throws IOException {
        logger.info("**** Start cleansing stops.");
        Stream<StopData> stops = dataReaderFactory.getForCleanser().getStops();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("stops");

        stops.filter(stop -> stopIds.contains(stop.getId())).forEach(stop -> {
            String tramPrefix = stop.isTram() ? StopDataParser.tramStation : "";
            writer.writeLine(String.format("%s,%s,\"%s,%s%s\",%s,%s",
                    stop.getId(),
                    stop.getCode(),
                    stop.getArea(),
                    workAroundName(stop.getName()), tramPrefix,
                    stop.getLatitude(),
                    stop.getLongitude()));
        });

        writer.close();
        stops.close();
        logger.info("**** End cleansing stops.\n\n");
    }

    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    public List<String> cleanseRoutes(Set<String> agencyCodes) throws IOException {
        logger.info("**** Start cleansing routes");
        List<String> routeCodes = new LinkedList<>();
        Stream<RouteData> routes = dataReaderFactory.getForCleanser().getRoutes();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("routes");

        if ((agencyCodes.size()==1) && (agencyCodes.contains(WILDCARD))) {
            logger.info("Adding all routes");
            routes.forEach(route -> addRoute(routeCodes, writer, route));

        } else {
            logger.info("Adding filtered routes");
            routes.filter(route -> agencyCodes.contains(route.getAgency())).forEach(route ->
                    addRoute(routeCodes, writer, route));
        }
        writer.close();
        routes.close();
        logger.info("**** End cleansing routes.\n\n");
        return routeCodes;
    }

    private void addRoute(List<String> routeCodes, TransportDataWriter writer, RouteData route) {
        String id = route.getId();
        String routeName = route.getName();

        if (config.getRemoveRouteNameSuffix()) {
            int indexOf = routeName.indexOf("(");
            if (indexOf > -1) {
                routeName = routeName.substring(0,indexOf).trim();
            }
        }
        writer.writeLine(String.format("%s,%s,%s,%s,0",
                id,
                route.getAgency(),
                route.getCode(),
                routeName));
        routeCodes.add(id);
        logger.info("Added route " + id);
    }

    private String runsOnDay(boolean day) {
        return day ? "1" : "0";
    }

    public void cleanFeedInfo() throws IOException {
        logger.info("**** Start cleansing feed info.");
        Stream<FeedInfo> feedInfo = dataReaderFactory.getForCleanser().getFeedInfo();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("feed_info");

        feedInfo.forEach(info -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                info.getPublisherName(),
                info.getPublisherUrl(),
                info.getTimezone(),
                info.getLang(),
                info.validFrom().format(DATE_FORMAT),
                info.validUntil().format(DATE_FORMAT),
                info.getVersion())));
        writer.close();
        feedInfo.close();
        logger.info("**** End cleansing feed info.");

    }

}
