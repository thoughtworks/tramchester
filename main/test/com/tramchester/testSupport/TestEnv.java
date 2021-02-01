package com.tramchester.testSupport;

import com.codahale.metrics.Gauge;
import com.tramchester.CacheMetrics;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.*;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.testSupport.reference.TramStations;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEnv {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestEnv.class);

    public static final int DAYS_AHEAD = 7;
    private static final LocalDate testDay;
    private static final LocalDate saturday;
    private static final LocalDate sunday;
    private static final LocalDate monday;

    public static final LatLong nearAltrincham = new LatLong(53.387483D, -2.351463D);
    public static final LatLong nearAltrinchamInterchange = new LatLong(53.3873279D,-2.3498573D);
    public static final LatLong nearPiccGardens = new LatLong(53.4805248D, -2.2394929D);
    public static final LatLong nearShudehill = new LatLong(53.485846, -2.239472);
    public static final LatLong nearStockportBus = new LatLong(53.408735,-2.1656593);
    public static final LatLong nearGreenwich = new LatLong(51.477928, -0.001545);
    public static final LatLong nearKnutsfordBusStation = new LatLong(53.3026112D,-2.3774635D);

    public static final GridPosition nearAltrinchamGrid;
    public static final GridPosition nearPiccGardensGrid;
    public static final GridPosition nearShudehillGrid;
    public static final GridPosition nearStockportBusGrid;
    public static final GridPosition nearGreenwichGrid;

    public static DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static DateTimeFormatter dateFormatSimple = DateTimeFormatter.ofPattern("ddMMyyyy");
    public static Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    private static final Agency MET = new Agency(DataSourceID.TFGM(), "MET", "agencyName");
    public static final Agency ArrivaTrainsWales = new Agency(DataSourceID.GBRail(), "AW", "Arriva Trains Wales");
    public final static HashSet<GTFSTransportationType> tramAndBus =
            new HashSet<>(Arrays.asList(GTFSTransportationType.tram, GTFSTransportationType.bus));


    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return Collections.emptyList();
            }
        };
    }

    public static TramchesterConfig GET(TestLiveDataConfig testLiveDataConfig) {
        return new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return null;
            }

            @Override
            public LiveDataConfig getLiveDataConfig() {
                return testLiveDataConfig;
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
        nearAltrinchamGrid = CoordinateTransforms.getGridPosition(nearAltrincham);
        nearPiccGardensGrid = CoordinateTransforms.getGridPosition(nearPiccGardens);
        nearShudehillGrid = CoordinateTransforms.getGridPosition(nearShudehill);
        nearStockportBusGrid = CoordinateTransforms.getGridPosition(nearStockportBus);
        nearGreenwichGrid = CoordinateTransforms.getGridPosition(nearGreenwich);
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
        return getTestRoute(StringIdFor.createId("RouteId"));
    }

    public static Route getTestRoute(IdFor<Route> routeId) {
        return new Route(routeId, "routeCode", "routeName", MetAgency(), TransportMode.Tram, RouteDirection.Inbound);
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

    public static PlatformStopCall createTramStopCall(IdFor<Trip> tripId, String stopId, TramStations station, int seq,
                                                      TramTime arrive, TramTime depart) {
        return createTramStopCall(tripId.forDTO(), stopId, station, seq, arrive, depart);
    }

    public static PlatformStopCall createTramStopCall(String tripId, String stopId, TramStations station, int seq, TramTime arrive,
                                                      TramTime depart) {
        Platform platform = createPlatform(stopId, station.getLatLong());
        GTFSPickupDropoffType pickupDropoff = GTFSPickupDropoffType.Regular;
        StopTimeData stopTimeData = new StopTimeData(tripId, arrive, depart, stopId, seq, pickupDropoff, pickupDropoff);
        return new PlatformStopCall(platform, TramStations.of(station), stopTimeData);
    }

    private static Platform createPlatform(String id, LatLong latLong) {
        return new Platform(id, "name:"+ id, latLong);
    }

    public static BoundingBox getTFGMBusBounds() {
        return new BoundingBox(333200, 373250, 414500, 437850);
    }

    public static BoundingBox getTrainBounds() {
        return new BoundingBox(147588, 30599, 654747, 967921);
    }


    public static LatLong stPetersSquareLocation() {
        return new LatLong(53.47825,-2.24314);
    }

    public static LatLong nearWythenshaweHosp() {
        return new LatLong(53.3874309,-2.2945628);
    }

    public static String postcodeForWythenshaweHosp() {
       return "M239LT";
    }

    public static IdFor<RouteStation> formId(TramStations tramStations, KnownTramRoute knownRoute) {
        return RouteStation.formId(tramStations.getId(), knownRoute.getId());
    }

    public static <T extends GraphProperty> void assertIdEquals(HasId<T> itemA, HasId<T> itemB) {
        assertEquals(itemA.getId(), itemB.getId());
    }

    public static CacheMetrics.RegistersCacheMetrics NoopRegisterMetrics() {
        return new CacheMetrics.RegistersCacheMetrics() {
            @Override
            public <T> void register(String metricName, Gauge<T> Gauge) {
                // noop
            }
        };
    }

    public static Set<DayOfWeek> allDays() {
        return new HashSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }


}
