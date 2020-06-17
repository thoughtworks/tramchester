package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    public static DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("YYYY-MM-dd");
    public static DateTimeFormatter dateFormatSimple = DateTimeFormatter.ofPattern("ddMMYYYY");
    public static Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    private static final Agency MET = new Agency("MET");

    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;
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

}
