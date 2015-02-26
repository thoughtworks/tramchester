package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.data.TripData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DataCleanser {
    private static final String path = "data/new/";
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    private static final TransportDataReader transportDataReader = new TransportDataReader(path);
    private static final TransportDataWriter transportDataWriter = new TransportDataWriter(path);

    public static void main(String[] args) throws Exception {
        cleanseRoutes();
        cleanseStops();
        cleanseTrips();

        String filename;


//        List<StopTimeData> stopTimes = getStopTimes();
//
//
//        String stopTimeSting = "";
//        int i = 0;
//        for (StopTimeData stopTime : stopTimes) {
//            if (stopTime.getStopId().startsWith("9400Z")) {
//                stopTimeSting += stopTime.getTripId() + "," + DateTimeService.formatTime(stopTime.getArrivalTime()) + "," + DateTimeService.formatTime(stopTime.getDepartureTime()) + "," + stopTime.getStopId() + "," + stopTime.getStopSequence() + "," + stopTime.getPickupType() + "," + stopTime.getDropOffType() + "\n";
//            }
//            i++;
//            if ((i % 10000) == 0) {
//                System.out.println("processed " + i + "/" + stopTimes.size() + " stop times!");
//            }
//        }
//        filename = "stop_times";
//        writeFile(stopTimeSting, filename);


        /////////////

//        List<TripData> trips1 = transportDataReader.getTrips();
//        Set<String> services = new HashSet<>();
//        for (TripData tripData : trips1) {
//            services.add(tripData.getServiceId());
//        }
//        List<CalendarData> calendar = transportDataReader.getCalendar();
//
//        String calendarSting = "";
//        for (CalendarData calendarData : calendar) {
//            if (services.contains(calendarData.getServiceId())) {
//                calendarSting += calendarData.getServiceId() + "," + getInt(calendarData.isMonday())+ "," + getInt(calendarData.isTuesday())
//                        + "," + getInt(calendarData.isWednesday())
//                        + "," + getInt(calendarData.isThursday())
//                        + "," + getInt(calendarData.isFriday())
//                        + "," + getInt(calendarData.isSaturday())
//                        + "," + getInt(calendarData.isSunday())
//                        + "," + calendarData.getStart().toString("YYYMMdd")+ "," + calendarData.getEnd().toString("YYYMMdd") + "\n";
//            }
//        }
//        filename = "calendar";
//        transportDataWriter.writeFile(calendarSting, filename);
        System.out.println("Done!!!");
    }

    private static void cleanseTrips() throws IOException {
        logger.info("**** Start cleansing trips.");
        List<TripData> trips = transportDataReader.getTrips();

        StringBuilder content = new StringBuilder();
        for (TripData trip : trips) {
            if (trip.getRouteId().startsWith("MET")) {
                content.append(String.format("%s,%s,%s,%s\n",
                        trip.getRouteId(),
                        trip.getServiceId(),
                        trip.getTripId(),
                        trip.getTripHeadsign()));
            }
        }
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

    private static String getInt(boolean monday) {
        return monday ? "1" : "0";
    }


}
