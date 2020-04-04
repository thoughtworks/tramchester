package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.SummaryResult;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tramchester.acceptance.AppUserJourneyTest.desiredJourney;
import static com.tramchester.acceptance.AppUserJourneyTest.validateAStage;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class AppUserJourneyLoactionsTest {
    private static final String configPath = "config/localAcceptance.yml";
    private static DriverFactory driverFactory;

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    private final String bury = Stations.Bury.getName();
    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();

    @Rule
    public TestName testName = new TestName();

    private LocalDate nextTuesday;
    private String url;
    private ProvidesDriver providesDriver;

    private static List<String> getBrowserList() {
        if (!TestEnv.isCircleci()) {
            return Arrays.asList("chrome", "firefox");
        }
        // TODO - confirm this is still an issue
        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
        return Collections.singletonList("firefox");
    }

    @Parameterized.Parameters(name="{index}: {0}")
    public static Iterable<? extends Object> data() {
        return getBrowserList();
    }

    @Parameterized.Parameter
    public String browserName;

    @BeforeClass
    public static void beforeAnyTestsRun() {
        driverFactory = new DriverFactory();
    }

    @Before
    public void beforeEachTestRuns() throws IOException {
        url = testRule.getUrl()+"/app/index.html";

        providesDriver = driverFactory.get(true, browserName);
        providesDriver.setStubbedLocation(TestEnv.nearAltrincham);
        providesDriver.init();
        providesDriver.clearCookies();

        // TODO offset for when tfgm data is expiring
        nextTuesday = TestEnv.nextTuesday(0);
    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @AfterClass
    public static void afterAllTestsRun() {
        driverFactory.close();
        driverFactory.quit();
    }

    @Test
    public void shouldHaveCorrectNearbyStops() {
        AppPage appPage = prepare();

        assertTrue(appPage.hasLocation());
        assertTrue(appPage.searchEnabled());

        // from
        List<String> myLocationStops = appPage.getNearbyFromStops();
        assertEquals(1, myLocationStops.size());

        List<String> nearestFromStops = appPage.getNearestFromStops();
        assertThat("Have nearest stops", nearestFromStops, hasItems(altrincham, Stations.NavigationRoad.getName()));
        List<String> allFrom = appPage.getAllStopsFromStops();
        assertThat(allFrom, not(contains(nearestFromStops)));
        int recentFromCount = appPage.getRecentFromStops().size();
        assertEquals(Stations.NumberOf, nearestFromStops.size()+allFrom.size()+recentFromCount);

        // to
        List<String> myLocationToStops = appPage.getNearbyToStops();
        assertEquals(1, myLocationToStops.size());

        List<String> nearestToStops = appPage.getNearestFromStops();
        assertThat(nearestToStops, hasItems(altrincham, Stations.NavigationRoad.getName()));
        List<String> allTo = appPage.getAllStopsToStops();
        assertThat(allTo, not(contains(nearestToStops)));
        int recentToCount = appPage.getRecentToStops().size();
        assertEquals(Stations.NumberOf, nearestToStops.size()+allTo.size()+recentToCount);

        // check recents works as expected
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"), false);
        appPage.planAJourney();
        appPage.waitForReady();

        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(altrincham, bury));
        nearestFromStops = appPage.getNearestFromStops();
        assertThat(nearestFromStops, hasItems(Stations.NavigationRoad.getName()));
        // TODO to recent just bury, not alty
    }
    
    @Test
    public void shouldCheckNearAltrinchamToDeansgate() {
        AppPage appPage = prepare();
        LocalTime planTime = LocalTime.of(10,15);
        desiredJourney(appPage, "My Location", deansgate, nextTuesday, planTime, false);
        appPage.planAJourney();

        List<SummaryResult> results = appPage.getResults();
        // TODO lockdown timetable: 3 -> 2
        assertTrue("at least some results", results.size()>=2);

        for (SummaryResult result : results) {
            LocalTime departTime = result.getDepartTime();
            assertTrue(departTime.toString(), departTime.isAfter(planTime));

            LocalTime arriveTime = result.getArriveTime();
            assertTrue(arriveTime.toString(), arriveTime.isAfter(departTime));
            assertEquals("Direct", result.getChanges());
        }

        // select first journey
        SummaryResult firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());
        Stage firstStage = stages.get(0);

        // TODO lockdown 10:19 -> 10:25
        validateWalkingStage(firstStage, LocalTime.of(10,25), "Walk to",
                Stations.Altrincham.getName(), -1, "RouteWalking", "Walk", 0);

        Stage secondStage = stages.get(1);
        // TODO lockdown 10:25 -> 10:31
        LocalTime departTime = LocalTime.of(10,31);
        validateAStage(secondStage, departTime, "Board", Stations.Altrincham.getName(), 1,
                AppUserJourneyTest.altyToPiccClass, AppUserJourneyTest.altyToPicLineName, "Piccadilly", 9);
    }

    private void validateWalkingStage(Stage stage, LocalTime departTime, String action, String actionStation, int platform,
                                      String lineClass, String lineName, int stops) {
        assertEquals("departtime", departTime, stage.getDepartTime());
        assertEquals("action",action, stage.getAction());
        assertEquals("actionStation", actionStation, stage.getActionStation());
        assertEquals("platform", platform, stage.getPlatform());
        assertEquals("lineName", lineName, stage.getLine(lineClass));
        assertEquals("stops", stops, stage.getPassedStops());
    }

    private AppPage prepare() {
        return AppUserJourneyTest.prepare(providesDriver, url);
    }

}

