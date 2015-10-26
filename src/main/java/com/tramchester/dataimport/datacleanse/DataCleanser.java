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
    private static final String path = "data/tram/";
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    public static final String TIME_FORMAT = "YYYMMdd";
    public static final String METROLINK = "MET";
    public static final String METROLINK_STOP_PREFIX = "9400";

    private TransportDataReader transportDataReader; //= new TransportDataReader(path + "gtdf-out/");
    private TransportDataWriter transportDataWriter; //= new TransportDataWriter(path);
    private FetchDataFromUrl fetcher;

    public static void main(String[] args) throws Exception {
        TransportDataReader reader = new TransportDataReader(path + "gtdf-out/");
        TransportDataWriter writer = new TransportDataWriter(path);
        FetchDataFromUrl fetcher = new FetchDataFromUrl(path);

        DataCleanser dataCleanser = new DataCleanser(fetcher, reader, writer);
        dataCleanser.run("http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");
    }

    public DataCleanser(FetchDataFromUrl fetcher, TransportDataReader reader, TransportDataWriter writer) {
        this.fetcher = fetcher;
        this.transportDataReader = reader;
        this.transportDataWriter = writer;
    }

    private void run(String dataUrl) throws IOException {
        fetcher.fetchData(dataUrl);

        cleanseRoutes();

        cleanseStops();

        Set<String> svcIds = cleanseTrips();

        cleanseStoptimes();

        cleanseCalendar(svcIds);

        cleanFeedInfo();

        FileUtils.deleteDirectory(new File(path + "/gtdf-out/"));
        FileUtils.forceDelete(new File(path + "/data.zip"));
    }

    public void cleanseCalendar(Set<String> services) throws IOException {
        logger.info("**** Start cleansing calendar.");

        //Set<String> services = getUniqueServices();

        Stream<CalendarData> calendar = transportDataReader.getCalendar();

        StringBuilder content = new StringBuilder();
        calendar.filter(calendarData -> services.contains(calendarData.getServiceId()) && calendarData.runsAtLeastADay())
                .forEach(calendarData -> content.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
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

        transportDataWriter.writeFile(content.toString(), "calendar");
        logger.info("**** End cleansing calendar.\n\n");
    }

//    private Set<String> getUniqueServices() throws IOException {
//        return new TransportDataReader(path).getTrips().map(TripData::getServiceId).collect(Collectors.toSet());
//    }

    public void cleanseStoptimes() throws IOException {
        logger.info("**** Start cleansing stop times.");

        Stream<StopTimeData> stopTimes = transportDataReader.getStopTimes();

        StringBuilder content = new StringBuilder();

        stopTimes.filter(stopTime -> stopTime.getStopId().startsWith(METROLINK_STOP_PREFIX))
                .forEach(stopTime -> content.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        stopTime.getTripId(),
                        DateTimeService.formatTime(stopTime.getArrivalTime()),
                        DateTimeService.formatTime(stopTime.getDepartureTime()),
                        stopTime.getStopId(),
                        stopTime.getStopSequence(),
                        stopTime.getPickupType(),
                        stopTime.getDropOffType())));

        transportDataWriter.writeFile(content.toString(), "stop_times");
        logger.info("**** End cleansing stop times.\n");

    }

    public Set<String> cleanseTrips() throws IOException {
        logger.info("**** Start cleansing trips.");
        Set<String> uniqueSvcIds = new HashSet<>();
        Stream<TripData> trips = transportDataReader.getTrips();

        StringBuilder content = new StringBuilder();
        trips.filter(trip -> trip.getRouteId().startsWith(METROLINK)).forEach(trip -> {
            content.append(String.format("%s,%s,%s,%s\n",
                    trip.getRouteId(),
                    trip.getServiceId(),
                    trip.getTripId(),
                    trip.getTripHeadsign()));
            uniqueSvcIds.add(trip.getServiceId());
        });
        transportDataWriter.writeFile(content.toString(), "trips");
        logger.info("**** End cleansing trips.\n\n");
        return uniqueSvcIds;
    }

    public void cleanseStops() throws IOException {
        logger.info("**** Start cleansing stops.");
        Stream<StopData> stops = transportDataReader.getStops();
        StringBuilder content = new StringBuilder();

        stops.filter(stop -> stop.getId().startsWith(METROLINK_STOP_PREFIX)).forEach(stop -> content.append(String.format("%s,%s,%s,%s,%s\n",
                stop.getId(),
                stop.getCode(),
                stop.getName(),
                stop.getLatitude(),
                stop.getLongitude())));

        transportDataWriter.writeFile(content.toString(), "stops");
        logger.info("**** End cleansing stops.\n\n");
    }

    public void cleanseRoutes() throws IOException {
        logger.info("**** Start cleansing routes.");
        Stream<RouteData> routes = transportDataReader.getRoutes();
        StringBuilder content = new StringBuilder();

        routes.filter(route -> route.getAgency().equals(METROLINK)).forEach(route -> content.append(String.format("%s,%s,%s,%s,0\n",
                route.getId(),
                route.getAgency(),
                route.getCode(),
                route.getName()
        )));

        transportDataWriter.writeFile(content.toString(), "routes");
        logger.info("**** End cleansing routes.\n\n");
    }

    private String runsOnDay(boolean day) {
        return day ? "1" : "0";
    }

    public void cleanFeedInfo() throws IOException {
        logger.info("**** Start cleansing feed info.");
        Stream<FeedInfo> feedInfo = transportDataReader.getFeedInfo();
        StringBuilder content = new StringBuilder();

        feedInfo.skip(1).forEach(info -> content.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                info.getPublisherName(),
                info.getPublisherUrl(),
                info.getTimezone(),
                info.getLang(),
                info.validFrom(),
                info.validUntil(),
                info.getVersion())));
        transportDataWriter.writeFile(content.toString(), "feed_info");
        logger.info("**** End cleansing feed info.");

    }
}
