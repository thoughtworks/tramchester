package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.services.DateTimeService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;


public class DataCleanser {
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    public static final String TIME_FORMAT = "YYYMMdd";
    public static final String METROLINK = "MET";
    public static final String METROLINK_STOP_PREFIX = "9400";

    private TransportDataReader transportDataReader;
    private TransportDataWriterFactory transportDataWriterFactory;
    private FetchDataFromUrl fetcher;

    public static void main(String[] args) throws Exception {
        String path = "data/tram/";
        TransportDataReader reader = new TransportDataReader(path + "gtdf-out/");
        TransportDataWriterFactory writer = new TransportDataWriterFactory(path);
        FetchDataFromUrl fetcher = new FetchDataFromUrl(path, "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");

        DataCleanser dataCleanser = new DataCleanser(fetcher, reader, writer);
        dataCleanser.run();

        FileUtils.deleteDirectory(new File(path + "/gtdf-out/"));
        FileUtils.forceDelete(new File(path + "/data.zip"));
    }

    public DataCleanser(FetchDataFromUrl fetcher, TransportDataReader reader, TransportDataWriterFactory factory) {
        this.fetcher = fetcher;
        this.transportDataReader = reader;
        this.transportDataWriterFactory = factory;
    }

    private void run() throws IOException {
        fetcher.fetchData();

        cleanseRoutes();

        cleanseStops();

        Set<String> svcIds = cleanseTrips();

        cleanseStoptimes();

        cleanseCalendar(svcIds);

        cleanFeedInfo();

    }

    public void cleanseCalendar(Set<String> services) throws IOException {
        logger.info("**** Start cleansing calendar.");

        Stream<CalendarData> calendar = transportDataReader.getCalendar();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("calendar");
        calendar.filter(calendarData -> services.contains(calendarData.getServiceId()) && calendarData.runsAtLeastADay())
                .forEach(calendarData -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        calendarData.getServiceId(),
                        runsOnDay(calendarData.isMonday()),
                        runsOnDay(calendarData.isTuesday()),
                        runsOnDay(calendarData.isWednesday()),
                        runsOnDay(calendarData.isThursday()),
                        runsOnDay(calendarData.isFriday()),
                        runsOnDay(calendarData.isSaturday()),
                        runsOnDay(calendarData.isSunday()),
                        calendarData.getStartDate().toString(TIME_FORMAT),
                        calendarData.getEndDate().toString(TIME_FORMAT))));

        writer.close();
        logger.info("**** End cleansing calendar.\n\n");
    }

    public void cleanseStoptimes() throws IOException {
        logger.info("**** Start cleansing stop times.");

        Stream<StopTimeData> stopTimes = transportDataReader.getStopTimes();
        TransportDataWriter writer = transportDataWriterFactory.getWriter("stop_times");

        stopTimes.filter(stopTime -> stopTime.getStopId().startsWith(METROLINK_STOP_PREFIX))
                .forEach(stopTime -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                        stopTime.getTripId(),
                        DateTimeService.formatTime(stopTime.getArrivalTime()),
                        DateTimeService.formatTime(stopTime.getDepartureTime()),
                        stopTime.getStopId(),
                        stopTime.getStopSequence(),
                        stopTime.getPickupType(),
                        stopTime.getDropOffType())));

        writer.close();
        logger.info("**** End cleansing stop times.\n");

    }

    public Set<String> cleanseTrips() throws IOException {
        logger.info("**** Start cleansing trips.");
        Set<String> uniqueSvcIds = new HashSet<>();
        Stream<TripData> trips = transportDataReader.getTrips();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("trips");

        trips.filter(trip -> trip.getRouteId().startsWith(METROLINK)).forEach(trip -> {
            writer.writeLine(String.format("%s,%s,%s,%s",
                    trip.getRouteId(),
                    trip.getServiceId(),
                    trip.getTripId(),
                    trip.getTripHeadsign()));
            uniqueSvcIds.add(trip.getServiceId());
        });
        writer.close();
        logger.info("**** End cleansing trips.\n\n");
        return uniqueSvcIds;
    }

    public void cleanseStops() throws IOException {
        logger.info("**** Start cleansing stops.");
        Stream<StopData> stops = transportDataReader.getStops();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("stops");

        stops.filter(stop -> stop.getId().startsWith(METROLINK_STOP_PREFIX)).forEach(stop ->
                writer.writeLine(String.format("%s,%s,%s,%s,%s",
                        stop.getId(),
                        stop.getCode(),
                        stop.getName(),
                        stop.getLatitude(),
                        stop.getLongitude())));

        writer.close();
        logger.info("**** End cleansing stops.\n\n");
    }

    public void cleanseRoutes() throws IOException {
        logger.info("**** Start cleansing routes.");
        Stream<RouteData> routes = transportDataReader.getRoutes();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("routes");

        routes.filter(route -> route.getAgency().equals(METROLINK)).forEach(route ->
                writer.writeLine(String.format("%s,%s,%s,%s,0",
                        route.getId(),
                        route.getAgency(),
                        route.getCode(),
                        route.getName()
                )));
        writer.close();
        logger.info("**** End cleansing routes.\n\n");
    }

    private String runsOnDay(boolean day) {
        return day ? "1" : "0";
    }

    public void cleanFeedInfo() throws IOException {
        logger.info("**** Start cleansing feed info.");
        Stream<FeedInfo> feedInfo = transportDataReader.getFeedInfo();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("feed_info");

        feedInfo.skip(1).forEach(info -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                info.getPublisherName(),
                info.getPublisherUrl(),
                info.getTimezone(),
                info.getLang(),
                info.validFrom(),
                info.validUntil(),
                info.getVersion())));
        writer.close();
        logger.info("**** End cleansing feed info.");

    }
}
