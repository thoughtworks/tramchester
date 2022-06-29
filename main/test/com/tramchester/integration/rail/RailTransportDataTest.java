package com.tramchester.integration.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.*;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.repository.RailStationCRSRepository;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.integration.testSupport.rail.RailStationIds.Inverness;
import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static java.time.DayOfWeek.SUNDAY;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
public class RailTransportDataTest {

    private static IntegrationRailTestConfig config;
    private static GuiceContainerDependencies componentContainer;
    private RailStationCRSRepository crsRepository;
    private NaptanRepository naptanRepository;
    private GraphFilterActive filter;
    private LoadRailStationRecords loadRailStationRecords;
    private RailDataRecordFactory railDataRecordFactory;
    private UnzipFetchedData.Ready ready;
    private TransportDataContainer dataContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        crsRepository = componentContainer.get(RailStationCRSRepository.class);
        naptanRepository = componentContainer.get(NaptanRepository.class);
        filter = componentContainer.get(GraphFilterActive.class);
        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);
        loadRailStationRecords = componentContainer.get(LoadRailStationRecords.class);
        railDataRecordFactory = componentContainer.get(RailDataRecordFactory.class);
        ready = componentContainer.get(UnzipFetchedData.Ready.class);

        // getting station repository triggers full load of timetable data, slowing down these tests a LOT
        // stationRepository = componentContainer.get(StationRepository.class);

        dataContainer = new TransportDataContainer(providesNow, "testingOnly");
    }

    @AfterEach
    void afterEachTestRuns() {
        dataContainer.dispose();
    }

    @Test
    public void shouldLoadSingleServices() {
        String text = """
                BSNY690052210092212040000001 POO2H4324  124673105 EMU    075      S            P
                BX         SWYSW807000                                                         \s
                LOWATRLMN 1548 15482  MSL    TB                                                \s
                LIWATRLWC           1549H00000000                                              \s
                LIVAUXHLM 1551 1552      15511552         T                                    \s
                LICLPHMJM 1556 1557      1556155711 SL    T                                    \s
                LIERLFLD  1600 1600H     16001600         T                                    \s
                LIWDON    1603H1604H     160416048        T                                    \s
                LIRAYNSPK 1607 1607H     16071607         T                                    \s
                LINEWMLDN 1609H1610      16101610         T                                    \s
                LINRBITON 1613 1613H     16131613         T                                    \s
                LIKGSTON  1615H1616H     161616162        T                                    \s
                LIHAMWICK 1618 1618H     161816182        T                                    \s
                LITEDNGTN 1620H1621      16211621         T                                    \s
                LISHCKLGJ           1623 00000000                                              \s
                LIFULWELL 1625 1625H     16251625         T                                    \s
                LIHAMPTON 1628H1629      16291629         T                                    \s
                LIKMPTNPK 1632 1632H     16321632         T                                    \s
                LISUNBURY 1633H1634      16341634         T                                    \s
                LIUHALIFD 1635H1636      16361636         T                                    \s
                LTSHEPRTN 1639 1641      TF                                                    \s""";

        final TransportData dataContainer = loadForTimetableData(text);

        Set<Service> services = dataContainer.getServices();
        assertEquals(1, services.size());

        Set<Trip> trips = dataContainer.getTrips();
        assertEquals(1, trips.size());

        Trip trip = trips.iterator().next();

        StopCalls calls = trip.getStopCalls();
        // 19 - 2 passing records
        assertEquals(17, calls.numberOfCallingPoints(), calls.toString());

        assertEquals(StringIdFor.createId("WATRLMN"), calls.getFirstStop().getStation().getId());
        assertEquals(StringIdFor.createId("SHEPRTN"), calls.getLastStop().getStation().getId());

    }

    @Test
    void shouldCaptureCorrectlyBasedOnActivityTypes() {
        String text = """
                BSNY923142205162209091111100 POO2J40    122334000 DMUE   090      S            P
                BX         AWYAW716100                                                         \s
                LOMCHYNLT 0545 05452         TB                                                \s
                LIDOVYDPL 0550H0552H     00000000         A N                                  \s
                LIDOVYJN  0553 0554      055405542        T                                    \s
                LIBORTH   0603 0604      06040604         T                                    \s
                LIPANYPER           0608H00000000                                              \s
                LIBOWSTRT 0609 0610      06090610         T                                    \s
                LILANBDRN           0614 00000000                     1                        \s
                LTABRYSTH 0618 0620      TF                                                    \s""";


        final TransportData dataContainer = loadForTimetableData(text);

        Set<Trip> trips = dataContainer.getTrips();
        assertEquals(1, trips.size());

        Trip trip = trips.iterator().next();

        StopCalls calls = trip.getStopCalls();
        assertEquals(5, calls.numberOfCallingPoints(), calls.toString());
    }

    @Test
    void shouldReproIssueWithMissingCallingPoint() {
        String text = """
                BSNL316482203132203130000001 PXX1P52    122727100 EMU700 100D     B            O
                BX         TLYTL053600                                                         \s
                LOKNGX    1826 18268  F      TB                                                \s
                LIKNGXBEL           1828 00000000   SL                                         \s
                LIHLWYSJ            1829H00000000                                              \s
                LIFNPK    1831 1833H     183118337        T                                    \s
                LIALEXNDP           1837 000000004                                             \s
                LIGORDONH           1842 000000003                      2                      \s
                LIHFDN              1851H000000002  DL                                         \s
                LIMOLEJN            1853 00000000                                              \s
                LIBRAGJN            1857 00000000                                              \s
                LILNGYSJN           1858H00000000                                              \s
                LILNGYJN            1859 00000000   SL                                         \s
                LISTEVNGE 1901 1902H     190119024        T                                    \s
                LIHITCHIN 1907 1908      190719082  SL    T                                    \s
                LIARLSEY  1912H1913      19131913         T                                    \s
                LIBIGLSWD 1917H1918H     191819184        T                                    \s
                LISNDY    1921H1922      192219222  SL    T                                    \s
                LISTNEOTS 1928H1929H     192919291        T                                    \s
                LIHNTNGDN 1936 1940      193619403  SL SL T           1                        \s
                LIHOLME   1950 1952      19501952   FL    T                                    \s
                LIFLETTON           1956H00000000                                              \s
                LTPBRO    1959 19592     TF                                                    \s""";

        final TransportData dataContainer = loadForTimetableData(text);

        Set<Trip> trips = dataContainer.getTrips();
        assertEquals(1, trips.size());

        Trip trip = trips.iterator().next();

        StopCalls calls = trip.getStopCalls();

        // NOTE: HOLME isn't actually a station, so it actually fine that it is missing in this case
        // it should not be marked as 'T' for activity (dropoff and pickup)
        assertEquals(11-1, calls.numberOfCallingPoints(), calls.toString());
    }

    @Test
    void shouldCrossMidnightAndSetNextDayCorrect() {
        String text = """
                BSNN518662207302207300000010 5BR0B00    122974000                              N
                BX         NTYNT071000
                LOBNSLY   2320 2320          TB
                LTHDRSFLD 0010 0010      TF
                BSNN518672207302207300000010 5BR0B00    122974000                              N
                BX         NTYNT071100
                LOHDRSFLD 2255 2255          TB
                LIBNSLY   2345 2345      23450000   BUS   D
                LIMEADWHL 0010 0010      00100000         D
                LTSHEFFLD 0030 0030      TFD""";

        final TransportData dataContainer = loadForTimetableData(text);

        Set<Service> services = dataContainer.getServices();
        assertEquals(2, services.size());

        Service service = dataContainer.getServiceById(StringIdFor.createId("N51867:20220730:20220730"));

        TramTime startTime = service.getStartTime();
        TramTime finishTime = service.getFinishTime();

        assertFalse(startTime.isNextDay(), startTime.toString());
        assertTrue(finishTime.isNextDay(), finishTime.toString());

        assertTrue(finishTime.isAfter(startTime));
        assertFalse(finishTime.isBefore(startTime));

        // NOTE: below mirrors test that fails when all rail services are loaded.....
        // ref: RailTransportDataFromFilesTest
        Set<Service> allServices = dataContainer.getServices();

        Set<Service> badTimings = allServices.stream().
                filter(svc -> svc.getFinishTime().isBefore(svc.getStartTime())).
                collect(Collectors.toSet());

        String diagnostics = badTimings.stream().
                map(svc -> svc.getId() + " begin: " + svc.getStartTime() + " end: " + svc.getFinishTime() + " ").
                collect(Collectors.joining());

        assertTrue(badTimings.isEmpty(), diagnostics);

    }

    @Test
    void shouldHandleDirectStartToFinishNoStops() {
        String text = """
                BSNY598672205162212101111110 POO2N70    113575825 EMU385 100      S            P
                BX         SRYSR847400                                                         \s
                LOALLOA   2347 23471         TB                                                \s
                LICMUSLP            2349H00000000                                              \s
                LISTIRCHJ           2353H00000000                        H                     \s
                LTSTIRLNG 2355 23559     TFRM                                                  \s""";

        final TransportData dataContainer = loadForTimetableData(text);

        Set<Service> services = dataContainer.getServices();
        assertEquals(1, services.size());

        Set<Trip> trips = dataContainer.getTrips();
        assertEquals(1, trips.size());

        Trip trip = trips.iterator().next();

        StopCalls stopCalls = trip.getStopCalls();
        assertEquals(2, stopCalls.numberOfCallingPoints(), stopCalls.toString());
    }

    @Test
    void shouldHandleVeryLongAndComplex() {
        String text = """
                BSNC436112207032207030000001 PXZ1M1630021235580031D  385 125      SBA R        O
                BX         CSYCS300201                                                         \s
                LOIVRNESS 2026 20262         TB                                                \s
                LIMILBRNJ           2027H00000000   UH                                         \s
                LICRAHALL           2029 00000000                                              \s
                LICULDNMR           2036 00000000         X                                    \s
                LIMOYY              2047H00000000                                              \s
                LITOMATIN           2051H00000000                                              \s
                LISLOCHD  2058 2105      00000000         X A                                  \s
                LICARRBDG           2111 000000001                    1                        \s
                LIAVIEMRE 2118 2121      000021191        U                                    \s
                LIKNCRAIG           2128 00000000                                              \s
                LIKGUSSIE 2133 2135      000021332        U                                    \s
                LINWTM    2139H2141H     000021411        U                                    \s
                LIDALWHIN 2154 2156      000021541        U                                    \s
                LIDLNSPDL           2204 00000000                                              \s
                LIDLNCRDH           2209H00000000                     1                        \s
                LIBLARATH 2220H2222H     000022221        U                                    \s
                LIPTLCHRY 2232 2234      000022321        U                                    \s
                LIDUNKELD 2246H2248H     000022481        U                                    \s
                LISTNY              2258 00000000                     1                        \s
                LIPERTH   2306H2308H     000023064        U                                    \s
                LIHILTONJ           2313 00000000                                              \s
                LINBRG              2322H00000000                       2H                     \s
                LILADYBNK           2334 000000002                    1                        \s
                LITHRNTNJ           2343 00000000                                              \s
                LITHRNUPL 2345 0003      00000000         A                                    \s
                LITHRNTSJ           0004 00000000                                              \s
                LIKCLD              0008 000000001                                             \s
                LIBISLND            0014H000000001                                             \s
                LIIVRKTHG           0022 000000001                       H                     \s
                LIDLMYJN            0028 00000000                     1                        \s
                LIHAYMRWJ           0034H00000000   UN                                         \s
                LIHAYMRKT           0036 000000001  UN                                         \s
                LIPRNCSTG           0037H00000000   Z                                          \s
                CREDINBUR XZ1M1630021235580031E  92  080      SBA R                CS300207    \s
                LIEDINBUR 0039H0122      000000002  Z  Z  L -U                                 \s
                LIPRNCSTG2          0123H00000000   DS                                         \s
                LIHAYMRKT2          0125 000000004                                             \s
                LISLATEFD           0127 00000000                                              \s
                LIMDCLDRJ           0134H00000000                                              \s
                LICOBB712           0140H00000000                                              \s
                LIACHNGRY           0143 00000000                                              \s
                LICRSTRSE           0147H00000000                     1                        \s
                LICRSTRSS           0150H00000000                                              \s
                LIABINGTN           0205 00000000UM                       1                    \s
                LIBEATCKS           0212 00000000UM                                            \s
                LIBEATCK            0219H00000000UM                                            \s
                LILCKRBIE           0230 000000002                    20                       \s
                LIKRKP862           0300 00000000                                              \s
                LIGRETNAJ           0303 00000000                                              \s
                LICARLILE           0310H000000004                                             \s
                LIPNTH              0325H000000001                                             \s
                LIEDENVGL           0328H00000000                                              \s
                LISHAPSMT           0336 00000000                                              \s
                LITEBAY             0340 00000000                                              \s
                LIGRIGG             0346H00000000                                              \s
                LIOXENHLM           0352 000000001                    1                        \s
                LICRNFNJN           0402H00000000                                              \s
                LIMORCMSJ           0406 00000000                                              \s
                LILANCSTR           0407H00000000UFL                                           \s
                LIGSTANG            0416H00000000                     2                        \s
                LIPRSTNFJ           0427H00000000   UFL                                        \s
                LIPRST    0429 0435      042900004  UFLUFLD                                    \s
                LIPRSTRJN           0436H00000000   FL                                         \s
                LIEUXTONJ           0440H00000000                                              \s
                LIBALSHWL           0442 00000000                                              \s
                LIWIGANNW           0447H000000004                                             \s
                LIWIGASJN           0448 00000000                                              \s
                LISPRBJN            0448H00000000   SL                                         \s
                LIGOLBRNJ           0453H00000000   FL                                         \s
                LIWINWCKJ           0457 00000000                                              \s
                LIDALAMBR           0459H00000000                                              \s
                LIWRGTNBQ           0500 000000002                       H                     \s
                LIACGJN             0502 00000000                     1                        \s
                LIWEAVERJ           0507 00000000                       1H                     \s
                LIACBG              0510H00000000                       2H                     \s
                LIHARTFDJ           0514 00000000                         1                    \s
                LIWNSFD             0519 00000000   SL                1                        \s
                LICREWECY           0525H00000000                                              \s
                LICREWE   0527 0529      052700006  FL    D                                    \s
                LICREWBHJ           0531H00000000                                              \s
                LIMADELEY           0536H00000000   FL FL             1                        \s
                LINTNB              0545H00000000   FL FL                                      \s
                LISTAFFRD           0549H00000000UFLFL FL                                      \s
                LICOLWICH           0555 00000000   SL FL                                      \s
                LIRUGLYNJ           0557H00000000   SL SL                  H                   \s
                LILCHTNJ            0604 00000000   SL SL                                      \s
                LIAMNGTNJ           0610 00000000   SL SL                                      \s
                LINNTN              0620 000000005  SL SL             1                        \s
                LIRUGBTVJ           0632H00000000   SL SL                                      \s
                LIRUGBY   0634 0640H     000000005  FL SL A                                    \s
                LIHMTNJ             0642H00000000                                              \s
                LIWEEDON            0651 00000000   FL FL                                      \s
                LIHANSLPJ           0701 00000000   FL FL                                      \s
                LIMKNSCEN           0706 000000004  FL FL                                      \s
                LIBLTCHLY           0708 000000002  FL FL                                      \s
                LILEDBRNJ           0715 00000000   FL FL             1                        \s
                LITRING             0720H000000002  FL FL                                      \s
                LIBONENDJ           0725 00000000   FL FL                                      \s
                LIWATFDJ            0731 000000007  FL FL                                      \s
                LIHROW              0735H000000004  FL FL                                      \s
                LIWMBY              0738 000000004                    1                        \s
                LIWLSDWLJ           0741 00000000   FL FL                                      \s
                LICMDNJN            0744 00000000   D                                          \s
                LICMDNSTH           0744H00000000                                              \s
                LTEUSTON  0747 074915    TF                                                    \s""";

        final TransportData dataContainer = loadForTimetableData(text);

        Set<Service> services = dataContainer.getServices();
        assertEquals(1, services.size());

        IdFor<Service> serviceId = StringIdFor.createId("C43611:20220703:20220703OVERLAY");

        Service service = dataContainer.getServiceById(serviceId);

        assertNotNull(service);
        assertEquals(serviceId, service.getId());

        TramTime startTime = service.getStartTime();
        TramTime finishTime = service.getFinishTime();

        assertEquals(TramTime.of(20,26), startTime);
        assertEquals(TramTime.nextDay(7,49), finishTime);

        // dates
        DateRange range = service.getCalendar().getDateRange();
        LocalDate operatingDate = LocalDate.of(2022, 7, 3);
        assertEquals(operatingDate, range.getStartDate());
        assertEquals(EnumSet.of(SUNDAY), service.getCalendar().getOperatingDays());

        // trip and stops
        Set<Trip> trips = dataContainer.getTrips();
        assertEquals(1, trips.size());
        Trip trip = trips.iterator().next();
        assertEquals(service, trip.getService());

        StopCalls stopCalls = trip.getStopCalls();
        assertEquals(12, stopCalls.numberOfCallingPoints());

        assertEquals(Inverness.getId(), stopCalls.getFirstStop().getStationId());
        assertEquals(RailStationIds.LondonEuston.getId(), stopCalls.getLastStop().getStationId());

        List<StopCalls.StopLeg> legs = stopCalls.getLegs(false);
        assertEquals(11, legs.size());

        StopCalls.StopLeg firstLeg = legs.get(0);
        assertEquals(startTime, firstLeg.getDepartureTime());
        assertEquals(Inverness.getId(), firstLeg.getFirstStation().getId());

        StopCalls.StopLeg lastLeg = legs.get(10);
        assertEquals(LondonEuston.getId(), lastLeg.getSecondStation().getId());
        assertEquals(TramTime.nextDay(5,29), lastLeg.getDepartureTime());
    }

    @NotNull
    private TransportDataContainer loadForTimetableData(String text) {
        ProvidesRailTimetableRecords loadTimeTableRecords = new LocalRailRecords(config, railDataRecordFactory, ready, text);

        RailConfig railConfig = config.getRailConfig();

        RailTransportDataFromFiles.Loader loader = new RailTransportDataFromFiles.Loader(loadRailStationRecords, loadTimeTableRecords,
                crsRepository, naptanRepository, railConfig, filter);

        loader.loadInto(dataContainer, config.getBounds());
        return dataContainer;
    }

    private static class LocalRailRecords extends LoadRailTimetableRecords {
        private final String text;

        public LocalRailRecords(TramchesterConfig config, RailDataRecordFactory factory, UnzipFetchedData.Ready ready, String text) {
            super(config, factory, ready);
            this.text = text;
        }

        @Override
        public Stream<RailTimetableRecord> load() {
            Reader reader = new StringReader(text);
            return super.load(reader);
        }
    }
}
