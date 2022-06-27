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
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
public class RailTransportDataTest {

    private static IntegrationRailTestConfig config;
    private static GuiceContainerDependencies componentContainer;
    private RailStationCRSRepository crsRepository;
    private NaptanRepository naptanRepository;
    private GraphFilterActive filter;
    private ProvidesNow providesNow;
    private LoadRailStationRecords loadRailStationRecords;
    private RailDataRecordFactory railDataRecordFactory;
    private UnzipFetchedData.Ready ready;
    private StationRepository stationRepository;

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
        providesNow = componentContainer.get(ProvidesNow.class);
        loadRailStationRecords = componentContainer.get(LoadRailStationRecords.class);
        railDataRecordFactory = componentContainer.get(RailDataRecordFactory.class);
        ready = componentContainer.get(UnzipFetchedData.Ready.class);
        stationRepository = componentContainer.get(StationRepository.class);
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

        assertEquals(stationRepository.getStationById(StringIdFor.createId("WATRLMN")), calls.getFirstStop().getStation());
        assertEquals(stationRepository.getStationById(StringIdFor.createId("SHEPRTN")), calls.getLastStop().getStation());

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
        assertEquals(11, calls.numberOfCallingPoints(), calls.toString());
    }

    @NotNull
    private TransportDataContainer loadForTimetableData(String text) {
        ProvidesRailTimetableRecords loadTimeTableRecords = new LocalRailRecords(config, railDataRecordFactory, ready, text);

        RailConfig railConfig = config.getRailConfig();

        RailTransportDataFromFiles.Loader loader = new RailTransportDataFromFiles.Loader(loadRailStationRecords, loadTimeTableRecords,
                crsRepository, naptanRepository, railConfig, filter);

        final TransportDataContainer dataContainer = new TransportDataContainer(providesNow, "testingOnly");
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
