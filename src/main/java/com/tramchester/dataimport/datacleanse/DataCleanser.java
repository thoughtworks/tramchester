package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.services.DateTimeService;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DataCleanser {
    private static final String path = "data/tram/";
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    private static final TransportDataReader transportDataReader = new TransportDataReader(path + "gtdf-out/");
    private static final TransportDataWriter transportDataWriter = new TransportDataWriter(path);
    public static final String TIME_FORMAT = "YYYMMdd";

    public static void main(String[] args) throws Exception {
        fetchData("http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");

        cleanseRoutes();

        cleanseStops();

        cleanseTrips();

        cleanseStoptimes();

        cleanseCalendar();

        cleanFeedInfo();

        FileUtils.deleteDirectory(new File(path + "/gtdf-out/"));
        FileUtils.forceDelete(new File(path + "/data.zip"));
    }



    private static void fetchData(String dataUrl) throws IOException {
        String filename = "data.zip";
        pullDataFromURL(filename, new URL(dataUrl));
        unzipData(filename);
    }

    private static void unzipData(String filename) {
        logger.info("Unziping data...");
        try {
            ZipFile zipFile = new ZipFile(path + filename);
            zipFile.extractAll(path);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    private static void pullDataFromURL(String filename, URL website) throws IOException {
        logger.info("Downloading data from " + website);

        FileUtils.forceMkdir(new File(path));
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(path + filename);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    private static void cleanseCalendar() throws IOException {
        logger.info("**** Start cleansing calendar.");

        Set<String> services = getUniqueServices();

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
                        calendarData.getStart().toString(TIME_FORMAT),
                        calendarData.getEnd().toString(TIME_FORMAT))));


        transportDataWriter.writeFile(content.toString(), "calendar");
        logger.info("**** End cleansing calendar.\n\n");
    }

    private static Set<String> getUniqueServices() throws IOException {
        return new TransportDataReader(path).getTrips().map(TripData::getServiceId).collect(Collectors.toSet());
    }

    private static void cleanseStoptimes() throws IOException {
        logger.info("**** Start cleansing stop times.");

        Stream<StopTimeData> stopTimes = transportDataReader.getStopTimes();

        StringBuilder content = new StringBuilder();

        stopTimes.filter(stopTime -> stopTime.getStopId().startsWith("9400Z"))
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

    private static void cleanseTrips() throws IOException {
        logger.info("**** Start cleansing trips.");
        Stream<TripData> trips = transportDataReader.getTrips();

        StringBuilder content = new StringBuilder();
        trips.filter(trip -> trip.getRouteId().startsWith("MET")).forEach(trip -> content.append(String.format("%s,%s,%s,%s\n",
                trip.getRouteId(),
                trip.getServiceId(),
                trip.getTripId(),
                trip.getTripHeadsign())));
        transportDataWriter.writeFile(content.toString(), "trips");
        logger.info("**** End cleansing trips.\n\n");
    }

    private static void cleanseStops() throws IOException {
        logger.info("**** Start cleansing stops.");
        Stream<StopData> stops = transportDataReader.getStops();
        StringBuilder content = new StringBuilder();

        stops.filter(stop -> stop.getId().startsWith("9400")).forEach(stop -> content.append(String.format("%s,%s,%s,%s,%s\n",
                stop.getId(),
                stop.getCode(),
                stop.getName(),
                stop.getLatitude(),
                stop.getLongitude())));

        transportDataWriter.writeFile(content.toString(), "stops");
        logger.info("**** End cleansing stops.\n\n");
    }

    private static void cleanseRoutes() throws IOException {
        logger.info("**** Start cleansing routes.");
        Stream<RouteData> routes = transportDataReader.getRoutes();
        StringBuilder content = new StringBuilder();

        routes.filter(route -> route.getId().startsWith("MET")).forEach(route -> content.append(String.format("%s,MET,%s,%s,0\n",
                route.getId(),
                route.getCode(),
                route.getName()
        )));

        transportDataWriter.writeFile(content.toString(), "routes");
        logger.info("**** End cleansing routes.\n\n");
    }

    private static String runsOnDay(boolean monday) {
        return monday ? "1" : "0";
    }

    private static void cleanFeedInfo() throws IOException {
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
