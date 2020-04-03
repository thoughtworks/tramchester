package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestEnv {
    public static final LatLong nearAltrincham = new LatLong(53.387483D, -2.351463D);
    public static final LatLong nearPiccGardens = new LatLong(53.4805248D, -2.2394929D);
    public static final LatLong nearShudehill = new LatLong(53.485846, -2.239472);
    public static DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("YYYY-MM-dd");
    public static DateTimeFormatter dateFormatSimple = DateTimeFormatter.ofPattern("ddMMYYYY");
    public static Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    private static Agency MET = new Agency("MET");

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

    public static LocalDate nextTuesday(int offsetDays) {
        DayOfWeek dayOfWeek = DayOfWeek.TUESDAY;
        LocalDate date = LocalNow().toLocalDate().plusDays(offsetDays);
        return getNextDate(dayOfWeek, date);
    }

    private static LocalDate getNextDate(DayOfWeek dayOfWeek, LocalDate date) {
        while (date.getDayOfWeek()!= dayOfWeek) {
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

    public static LocalDate nextSaturday() {
        return getNextDate(DayOfWeek.SATURDAY, LocalNow().toLocalDate());
    }

    public static LocalDate nextSunday() {
        return getNextDate(DayOfWeek.SUNDAY, LocalNow().toLocalDate());
    }

    public static boolean isCircleci() {
        return System.getenv("CIRCLECI") != null;
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
}
