package com.tramchester.testSupport;

import com.codahale.metrics.Gauge;
import com.tramchester.ComponentContainer;
import com.tramchester.caching.DataCache;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.id.StringIdFor.createId;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEnv {
    public static final Path CACHE_DIR = Path.of("testData","cache");
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestEnv.class);

    public static final int DAYS_AHEAD = 7;

    private static final TramDate testDay;
    private static final TramDate saturday;
    private static final TramDate sunday;
    private static final TramDate monday;

    public static final DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter dateFormatSimple = DateTimeFormatter.ofPattern("ddMMyyyy");
    public static final Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    public static final String BRISTOL_BUSSTOP_OCTOCODE = "0100BRP90268";

    private static final Agency MET = MutableAgency.build(DataSourceID.tfgm, MutableAgency.METL, "Metrolink");

    public static final Agency StagecoachManchester = MutableAgency.build(DataSourceID.tfgm, createId("SCMN"),
            "Stagecoach Manchester");
    public static final Agency WarringtonsOwnBuses = MutableAgency.build(DataSourceID.tfgm, createId("WBTR"),
            "Warrington's Own Buses");

    public static final String MANCHESTER_AIRPORT_BUS_AREA = "180GMABS";

    public static final String BACKUP_TIMETABLE_URL = "https://tramchester2dist.s3.eu-west-1.amazonaws.com/XXXX/tfgm_data.zip";

    public static final String TFGM_TIMETABLE_URL = "https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip";

    public static final String NAPTAN_BASE_URL = "https://naptan.api.dft.gov.uk/v1/access-nodes"; // ?dataFormat=csv

    public final static HashSet<GTFSTransportationType> tramAndBus =
            new HashSet<>(Arrays.asList(GTFSTransportationType.tram, GTFSTransportationType.bus));

    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.emptyList();
            }
        };
    }

    public static TramchesterConfig GET(TestTramLiveDataConfig testLiveDataConfig) {
        return new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return null;
            }

            @Override
            public TfgmTramLiveDataConfig getLiveDataConfig() {
                return testLiveDataConfig;
            }
        };
    }

    public static LocalDateTime LocalNow() {
        return LocalDateTime.now(TestConfig.TimeZoneId);
    }

    static {
        TramDate today = TramDate.from(LocalNow());
        testDay = getNextDate(DayOfWeek.THURSDAY, today);
        saturday = getNextDate(DayOfWeek.SATURDAY, today);
        sunday = getNextDate(DayOfWeek.SUNDAY, today);
        monday = getNextDate(DayOfWeek.MONDAY, today);
    }

    public static TramDate nextSaturday() {
        return saturday;
    }

    public static TramDate nextSunday() {
        return sunday;
    }

    public static TramDate testDay() {
        return testDay;
    }

    /***
     * use testDay()
     * @return test day
     */
    @Deprecated
    public static TramDate testTramDay() {
        return testDay;
    }

    public static TramDate nextMonday() {
        return monday;
    }

    private static TramDate getNextDate(DayOfWeek dayOfWeek, TramDate date) {
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return avoidChristmasDate(date);
    }

    public static TramDate avoidChristmasDate(TramDate date) {
        while (date.isChristmasPeriod()) {
            date = date.plusWeeks(1);
        }
        return date;
    }

    public static Stream<TramDate> getUpcomingDates() {
        TramDate baseDate = testDay();
        return DateRange.of(baseDate, baseDate.plusDays(DAYS_AHEAD)).stream();
    }

    public static Route getTramTestRoute() {
        return getTramTestRoute(createId("RouteId"), "routeName");
    }

    public static Route getTramTestRoute(IdFor<Route> routeId, String routeName) {
        return MutableRoute.getRoute(routeId, "routeCode", routeName, TestEnv.MetAgency(), TransportMode.Tram);
    }

    public static Route getTrainTestRoute(IdFor<Route> routeId, String routeName) {
        return MutableRoute.getRoute(routeId, "routeCode", routeName, TestEnv.NorthernTrainsAgency(), TransportMode.Train);
    }

    public static Agency MetAgency() {
        return MET;
    }

    public static Agency NorthernTrainsAgency() {
        return MutableAgency.build(DataSourceID.rail, TrainOperatingCompanies.NT.getAgencyId(), TrainOperatingCompanies.NT.getCompanyName());
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
            logger.info(format("Env var %s set to '%s' resulting in path '%s'", envVarName, value, path));
        }
        else {
            logger.error(format("Env var %s set to '%s' resulting in MISSING path '%s'", envVarName, value, path));
        }
        if (Files.isDirectory(path)) {
            logger.error(format("Env var %s set to '%s' resulting in DIRECTORY path '%s'", envVarName, value, path));
            return null;
        }
        return path;
    }

    public static boolean isCircleci() {
        return System.getenv("CIRCLECI") != null;
    }

    public static PlatformStopCall createTramStopCall(Trip trip, String stopId, TramStations tramStation, int seq, TramTime arrive,
                                                      TramTime depart) {
        final Station station = tramStation.fake();

        Platform platform = MutablePlatform.buildForTFGMTram(stopId, station, tramStation.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        GTFSPickupDropoffType pickupDropoff = GTFSPickupDropoffType.Regular;

        return new PlatformStopCall(platform, station, arrive, depart, seq, pickupDropoff, pickupDropoff, trip);
    }

    public static BoundingBox getTFGMBusBounds() {
        return new BoundingBox(333200, 373250, 414500, 437850);
    }

    public static BoundingBox getTrainBounds() {
        return new BoundingBox(147588, 30599, 654747, 967921);
    }

    public static BoundingBox getGreaterManchester() { return new BoundingBox(370000, 380000, 398500, 414500); }

    public static LatLong stPetersSquareLocation() {
        return new LatLong(53.47825,-2.24314);
    }


    public static CacheMetrics.RegistersCacheMetrics NoopRegisterMetrics() {
        return new CacheMetrics.RegistersCacheMetrics() {
            @Override
            public <T> void register(String metricName, Gauge<T> Gauge) {
                // noop
            }
        };
    }

    public static EnumSet<DayOfWeek> allDays() {
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    public static void deleteDBIfPresent(TramchesterConfig config) throws IOException {
        Path dbPath = config.getGraphDBConfig().getDbPath();
        if (Files.exists(dbPath)) {
            FileUtils.deleteDirectory(dbPath.toFile());
        }
    }

    public static void assertLatLongEquals(LatLong expected, LatLong actual, double delta, String message) {
        assertEquals(expected.getLat(), actual.getLat(), delta, "lat:" + message);
        assertEquals(expected.getLon(), actual.getLon(), delta, "lon: " +message);
    }

    public static void assertMinutesEquals(int minutes, Duration duration) {
        assertEquals(Duration.ofMinutes(minutes), duration, "Duration %s did match %d minutes".formatted(duration, minutes));
    }

    public static void assertMinutesEquals(int minutes, Duration duration, String message) {
        assertEquals(Duration.ofMinutes(minutes), duration, message);
    }

    public static void clearDataCache(ComponentContainer componentContainer) {
        DataCache cache = componentContainer.get(DataCache.class);
        cache.clearFiles();
    }

    public static int calcCostInMinutes(Location<?> stationA, Location<?> stationB, double mph) {
        double distanceInMiles = distanceInMiles(stationA.getLatLong(), stationB.getLatLong());
        double hours = distanceInMiles / mph;
        return (int)Math.ceil(hours * 60D);
    }

    private static double distanceInMiles(LatLong point1, LatLong point2) {

        final double EARTH_RADIUS = 3958.75;

        double lat1 = point1.getLat();
        double lat2 = point2.getLat();
        double diffLat = Math.toRadians(lat2-lat1);
        double diffLong = Math.toRadians(point2.getLon()-point1.getLon());
        double sineDiffLat = Math.sin(diffLat / 2D);
        double sineDiffLong = Math.sin(diffLong / 2D);

        double a = Math.pow(sineDiffLat, 2) + Math.pow(sineDiffLong, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double fractionOfRadius = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * fractionOfRadius;
    }

    public static Platform createPlatformFor(Station station, String platformId) {
        return MutablePlatform.buildForTFGMTram(platformId, station, station.getLatLong(),
                station.getDataSourceID(), station.getAreaId());
    }

    public static Platform onlyPlatform(Station station) {
        if (!station.hasPlatforms()) {
            throw new RuntimeException("No platforms");
        }
        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        if (platforms.size()!=1) {
            throw new RuntimeException("Wrong number of platforms " + platforms.size());
        }
        return platforms.get(0);
    }

}
