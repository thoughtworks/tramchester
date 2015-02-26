package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.services.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataCleanser {
    private static final String path = "data/new/";
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    private static final TransportDataReader transportDataReader = new TransportDataReader(path);
    private static final TransportDataWriter transportDataWriter = new TransportDataWriter(path);
    public static final String TIME_FORMAT = "YYYMMdd";

    public static void main(String[] args) throws Exception {
        cleanseRoutes();

        cleanseStops();

        cleanseTrips();

        cleanseStoptimes();

        cleanseCalendar();
    }

    private static void cleanseCalendar() throws IOException {
        logger.info("**** Start cleansing calendar.");

        Set<String> services = getUniqueServices();

        List<CalendarData> calendar = transportDataReader.getCalendar();

        StringBuilder content = new StringBuilder();
        calendar.stream().filter(calendarData -> services.contains(calendarData.getServiceId()))
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
        return transportDataReader.getTrips().stream().map(TripData::getServiceId).collect(Collectors.toSet());
    }

    private static void cleanseStoptimes() throws IOException {
        logger.info("**** Start cleansing stop times.");

        List<StopTimeData> stopTimes = transportDataReader.getStopTimes();

        StringBuilder content = new StringBuilder();

        stopTimes.stream().filter(stopTime -> stopTime.getStopId().startsWith("9400Z"))
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
        List<TripData> trips = transportDataReader.getTrips();

        StringBuilder content = new StringBuilder();
        trips.stream().filter(trip -> trip.getRouteId().startsWith("MET")).forEach(trip -> content.append(String.format("%s,%s,%s,%s\n",
                trip.getRouteId(),
                trip.getServiceId(),
                trip.getTripId(),
                trip.getTripHeadsign())));
        transportDataWriter.writeFile(content.toString(), "trips");
        logger.info("**** End cleansing trips.\n\n");
    }

    private static void cleanseStops() throws IOException {
        logger.info("**** Start cleansing stops.");
        List<StopData> stops = transportDataReader.getStops();
        StringBuilder content = new StringBuilder();

        stops.stream().filter(stop -> stop.getId().startsWith("9400")).forEach(stop -> content.append(String.format("%s,%s,%s,%s,%s\n",
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
        List<RouteData> routes = transportDataReader.getRoutes();
        StringBuilder content = new StringBuilder();

        routes.stream().filter(route -> route.getId().startsWith("MET")).forEach(route -> content.append(String.format("%s,MET,%s,%s,0\n",
                route.getId(),
                route.getCode(),
                route.getName()
        )));

        transportDataWriter.writeFile(content.toString(), "routes");
        logger.info("**** Start cleansing routes.\n\n");
    }

    private static String runsOnDay(boolean monday) {
        return monday ? "1" : "0";
    }


}
