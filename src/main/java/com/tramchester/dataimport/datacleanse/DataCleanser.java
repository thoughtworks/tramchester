package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.data.TripData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataCleanser {
    private static final String path = "data/new/";
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    private static final TransportDataReader transportDataReader= new TransportDataReader(path);
    private static final TransportDataWriter transportDataWriter= new TransportDataWriter(path);

    public static void main(String[] args) throws Exception {
        List<RouteData> routes = transportDataReader.getRoutes();
        String routeSting = cleanseRoutes(routes);
        String filename = "routes";
        transportDataWriter.writeFile(routeSting, filename);


        List<TripData> trips = transportDataReader.getTrips();

        String tripSting = "";
        for (TripData trip : trips) {
            if (trip.getRouteId().startsWith("MET")) {
                tripSting += trip.getRouteId() + "," + trip.getServiceId() + "," + trip.getTripId() + "," + trip.getTripHeadsign() + "\n";
            }
        }
        filename = "trips";
        transportDataWriter.writeFile(tripSting, filename);


        List<StopData> stops = transportDataReader.getStops();

        String stopSting = "";
        for (StopData stop : stops) {
            if (stop.getId().startsWith("9400")) {
                stopSting += stop.getId() + "," + stop.getCode() + "," + stop.getName() + "," + stop.getLatitude() + "," + stop.getLongitude() + "\n";

            }
        }
        filename = "stops";
        transportDataWriter.writeFile(stopSting, filename);


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

        List<TripData> trips1 = transportDataReader.getTrips();
        Set<String> services = new HashSet<>();
        for (TripData tripData : trips1) {
            services.add(tripData.getServiceId());
        }
        List<CalendarData> calendar = transportDataReader.getCalendar();

        String calendarSting = "";
        for (CalendarData calendarData : calendar) {
            if (services.contains(calendarData.getServiceId())) {
                calendarSting += calendarData.getServiceId() + "," + getInt(calendarData.isMonday())+ "," + getInt(calendarData.isTuesday())
                        + "," + getInt(calendarData.isWednesday())
                        + "," + getInt(calendarData.isThursday())
                        + "," + getInt(calendarData.isFriday())
                        + "," + getInt(calendarData.isSaturday())
                        + "," + getInt(calendarData.isSunday())
                        + "," + calendarData.getStart().toString("YYYMMdd")+ "," + calendarData.getEnd().toString("YYYMMdd") + "\n";
            }
        }
        filename = "calendar";
        transportDataWriter.writeFile(calendarSting, filename);
        System.out.println("Done!!!");
    }

    private static String getInt(boolean monday) {
        return monday ? "1" : "0";
    }

    private static String cleanseRoutes(List<RouteData> routes) {
        String routeSting = "";
        for (RouteData route : routes) {
            if (route.getId().startsWith("MET")) {
                routeSting += route.getId() + ", MET, " + route.getCode() + ", " + route.getName() + ",0 \n";
            }
        }
        return routeSting;
    }


}
