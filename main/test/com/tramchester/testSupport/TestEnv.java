package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.geo.BoundingBox;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;

public class TestEnv {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestEnv.class);

    public static final int DAYS_AHEAD = 7;
    private static final LocalDate testDay;
    private static final LocalDate saturday;
    private static final LocalDate sunday;
    private static final LocalDate monday;

    public static final LatLong nearAltrincham = new LatLong(53.387483D, -2.351463D);
    public static final LatLong nearPiccGardens = new LatLong(53.4805248D, -2.2394929D);
    public static final LatLong nearShudehill = new LatLong(53.485846, -2.239472);
    public static final LatLong nearStockportBus = new LatLong(53.408735,-2.1656593);
    public static final LatLong manAirportLocation = new LatLong(53.36535,-2.27247);
    public static final LatLong nearGreenwich = new LatLong(51.477928, -0.001545);

    public static DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("YYYY-MM-dd");
    public static DateTimeFormatter dateFormatSimple = DateTimeFormatter.ofPattern("ddMMYYYY");
    public static Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    private static final Agency MET = new Agency("MET", "agencyName");
    public final static HashSet<GTFSTransportationType> tramAndBus = new HashSet<>(Arrays.asList(GTFSTransportationType.tram, GTFSTransportationType.bus));;

    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return Collections.emptyList();
            }
        };
    }

    public static LocalDateTime LocalNow() {
        return LocalDateTime.now(TestConfig.TimeZone);
    }

    static {
        LocalDate today = LocalNow().toLocalDate();
        testDay = getNextDate(DayOfWeek.THURSDAY, today);
        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    public static LocalDate nextSaturday() {
        return saturday;
    }

    public static LocalDate nextSunday() {
        return sunday;
    }

    public static LocalDate testDay() {
        return testDay;
    }

    public static LocalDate nextMonday() {
        return monday;
    }

    private static LocalDate getNextDate(DayOfWeek dayOfWeek, LocalDate date) {
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return avoidChristmasDate(date);
    }

    public static LocalDate avoidChristmasDate(LocalDate date) {
        while (new TramServiceDate(date).isChristmasPeriod()) {
            date = date.plusWeeks(1);
        }
        return date;
    }

    public static Route getTestRoute() {
        return getTestRoute("RouteId");
    }

    public static Route getTestRoute(String routeId) {
        return new Route(routeId, "routeCode", "routeName", MetAgency(), TransportMode.Tram);
    }

    public static Agency MetAgency() {
        return MET;
    }

    // useful for diagnosing issues in windows env with spaces in paths etc.......
    public static Path getPathFromEnv(String envVarName) {
        String value = System.getenv(envVarName);
        if (value==null) {
            logger.warn(format("Environmental Variable %s not set", envVarName));
            return null;
        }
        Path path = Paths.get(value).toAbsolutePath();
        if (Files.exists(path)) {
            logger.info(format("Env var %s set to '%s' resulting in path '%s'", envVarName, value, path.toString()));
        }
        else {
            logger.error(format("Env var %s set to '%s' resulting in MISSING path '%s'", envVarName, value, path.toString()));
        }
        if (Files.isDirectory(path)) {
            logger.error(format("Env var %s set to '%s' resulting in DIRECTORY path '%s'", envVarName, value, path.toString()));
            return null;
        }
        return path;
    }

    public static boolean isCircleci() {
        return System.getenv("CIRCLECI") != null;
    }

    public static TramStopCall createTramStopCall(String tripId, String stopId, Station station, int seq, ServiceTime arrive, ServiceTime depart) {
        Platform platform = createPlatform(stopId);
        GTFSPickupDropoffType pickupDropoff = GTFSPickupDropoffType.Regular;
        StopTimeData stopTimeData = new StopTimeData(tripId, arrive, depart, stopId, seq, pickupDropoff, pickupDropoff);
        return new TramStopCall(platform, station, stopTimeData);
    }

    private static Platform createPlatform(String id) {
        return new Platform(id, "name:"+ id);
    }

    public static BoundingBox getTFGMBusBounds() {
        return new BoundingBox(333200, 373250, 414500, 437850);
    }

    public static BoundingBox getTrainBounds() {
        return new BoundingBox(147588, 30599, 654747, 967921);
    }


}
