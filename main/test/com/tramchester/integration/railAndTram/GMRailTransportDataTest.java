package com.tramchester.integration.railAndTram;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.LoadRailServicesFromText;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class GMRailTransportDataTest {

    private static TramchesterConfig config;
    private static GuiceContainerDependencies componentContainer;
    private TransportDataContainer dataContainer;
    private LoadRailServicesFromText loadRailServicesFromText;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        // getting station repository triggers full load of timetable data, slowing down these tests a LOT
        // stationRepository = componentContainer.get(StationRepository.class);

        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);
        UnzipFetchedData.Ready ready = componentContainer.get(UnzipFetchedData.Ready.class);

        dataContainer = new TransportDataContainer(providesNow, "testingOnly");

        loadRailServicesFromText = new LoadRailServicesFromText(config, componentContainer, ready);
    }

    @AfterEach
    void afterEachTestRuns() {
        dataContainer.dispose();
    }

    @Test
    void shouldLoadPiccadillyDirectToAirport() {
        String text = """
                BSNG906022205152209110000001 PXX1H427795112233110 DMUS   075      S S          P
                BX         NTYNT779500
                LOMNCRPIC 0729 07294  FL     TB
                LIARDWCKJ           0731 00000000
                LISLDLJN            0733H00000000
                LIHLDG              0739H000000002                      1
                LIHLDGWJ            0741H00000000
                LTMNCRIAP 0744 07454A    TF""";

        loadRailServicesFromText.loadInto(dataContainer, text);

        List<Service> services = new ArrayList<>(dataContainer.getServices());
        assertEquals(1, services.size());

        Service service = services.get(0);

        assertEquals(TramTime.of(7,29), service.getStartTime());
        assertEquals(TramTime.of(7,45), service.getFinishTime());

        assertFalse(service.intoNextDay());

        List<Trip> trips = new ArrayList<>(dataContainer.getTrips());

        assertEquals(1, trips.size());

        Trip trip = trips.get(0);

        StopCalls calls = trip.getStopCalls();
        assertEquals(2, calls.numberOfCallingPoints());

        assertEquals(ManchesterPiccadilly.getId(), calls.getFirstStop().getStationId());
        assertEquals(ManchesterAirport.getId(), calls.getLastStop().getStationId());
    }

    @Test
    public void shouldNotLoadOutsideOfArea() {
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

        loadRailServicesFromText.loadInto(dataContainer, text);

        Set<Service> services = dataContainer.getServices();
        assertEquals(0, services.size());
    }

    @Test
    void shouldReproIssueWithIncorrectTimingsWhenPartiallyWithinArea() {
        String text = """
                BSNL365492208082208111111000 PXX1M958061121730001 EMU397 125      B S T        O
                BX         TPYTP806100
                LOEDINBUR 2011 201114 W      TB
                LIPRNCSTG           2012 00000000   DS                  1
                LIHAYMRKT 2015 2016H     000020164        U             1H
                LISLATEFD           2020H00000000
                LIMDCLDRJ           2027 00000000
                LICOBB712           2032 00000000
                LIACHNGRY           2034 00000000                     1
                LICRSTRSE           2039 00000000
                LICRSTRSS           2040H00000000
                LIABINGTN           2050H00000000UM
                LIBEATCKS           2056 00000000UM
                LIBEATCK            2102 00000000UM
                LILCKRBIE 2111 2112H     211121122        T           2
                LIKRKP862           2123 00000000
                LIGRETNAJ           2125H00000000
                LICARLILE 2132H2135      213321354        T
                LIPNTH              2148 000000001
                LIEDENVGL           2150H00000000
                LISHAPSMT           2157H00000000
                LITEBAY             2200H00000000
                LIGRIGG             2204H00000000
                LIOXENHLM 2210 2211H     221022111        T           1
                LICRNFNJN           2220H00000000
                LIMORCMSJ           2223 00000000
                LILANCSTR 2225 2226H     222522264        T
                LIGSTANG            2234H00000000                     2
                LIPRSTNFJ           2242 00000000   UFL
                LIPRST    2243H2252H     224422524  UFLUFLT
                LIPRSTRJN           2253H00000000   FL                  2H
                LIEUXTONJ           2300 00000000                       3
                LICHORLEY           2306 00000000                       9H
                LILOSTCKJ           2320H00000000                       2H
                LIBOLTON  2326H2334H     000000003        OP          2 2
                LISLFDCT            2346 00000000UB
                LIORDSLLJ           2347 00000000
                LIWATSTJN           2347H00000000
                LIMNCRDGT           2348H000000001
                LIMNCROXR 2349H2351H     235023514        T
                LTMNCRPIC 2354 235413    TF""";

        loadRailServicesFromText.loadInto(dataContainer, text);

        List<Service> services = new ArrayList<>(dataContainer.getServices());
        assertEquals(1, services.size());

        List<Trip> trips = new ArrayList<>(dataContainer.getTrips());

        assertEquals(1, trips.size());

        Trip trip = trips.get(0);

        StopCalls calls = trip.getStopCalls();
        assertEquals(2, calls.numberOfCallingPoints());

        assertEquals(Station.createId("BOLTON"), calls.getFirstStop().getStationId());
        assertEquals(ManchesterPiccadilly.getId(), calls.getLastStop().getStationId());

        Service service = services.get(0);

        assertEquals(TramTime.of(23,26), service.getStartTime());
        assertEquals(TramTime.of(23,54), service.getFinishTime());

        assertFalse(service.intoNextDay());

    }

}
