package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.SummaryResult;
import com.tramchester.integration.Stations;
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
        if (!TestConfig.isCircleci()) {
            return Arrays.asList("chrome", "firefox");
        }
        // TODO - confirm this is still an issue
        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
        return Collections.singletonList("firefox");
    }

    @Parameterized.Parameters
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
        providesDriver.setStubbedLocation(TestConfig.nearAltrincham);
        providesDriver.init();
        providesDriver.clearCookies();

        // TODO offset for when tfgm data is expiring
        nextTuesday = TestConfig.nextTuesday(0);
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

        // from
        List<String> myLocationStops = appPage.getNearbyFromStops();
        assertEquals(1, myLocationStops.size());

        List<String> nearestFromStops = appPage.getNearestFromStops();
        assertThat(nearestFromStops, hasItems(altrincham, Stations.NavigationRoad.getName()));
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
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
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
        desiredJourney(appPage, "My Location", deansgate, nextTuesday, planTime);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        assertTrue(results.size()>=3);

        for (SummaryResult result : results) {
            assertTrue(result.getDepartTime().isAfter(planTime));
            assertTrue(result.getArriveTime().isAfter(result.getDepartTime()));
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
        validateWalkingStage(firstStage, LocalTime.of(10,19), "Walk to",
                Stations.Altrincham.getName(), -1, "RouteWalking", "Walk", 0);

        Stage secondStage = stages.get(1);
        LocalTime departTime = LocalTime.of(10,25);
        validateAStage(secondStage, departTime, "Board", Stations.Altrincham.getName(), 1,
                AppUserJourneyTest.altyToPiccClass, AppUserJourneyTest.altyToPicLineName, "Piccadilly", 9);
    }

    private void validateWalkingStage(Stage stage, LocalTime departTime, String action, String actionStation, int platform,
                                      String lineClass, String lineName, int stops) {
        assertEquals(departTime, stage.getDepartTime());
        assertEquals(action, stage.getAction());
        assertEquals(actionStation, stage.getActionStation());
        assertEquals(platform, stage.getPlatform());
        assertEquals(lineName, stage.getLine(lineClass));
        assertEquals(stops, stage.getPassedStops());
    }

    private AppPage prepare() {
        return AppUserJourneyTest.prepare(providesDriver, url);
    }

}

