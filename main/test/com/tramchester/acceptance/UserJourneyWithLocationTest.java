package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.acceptance.infra.*;
import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.JourneyPlannerHelper;

import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.acceptance.UserJourneyTest.getBrowserList;
import static junit.framework.TestCase.assertTrue;

@RunWith(Parameterized.class)
public class UserJourneyWithLocationTest {
    private static final String configPath = "config/localAcceptance.yml";
    private static DriverFactory driverFactory;
    private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    private String myLocation = "My Location";
    private LocalDate when;
    private String url;
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() { return getBrowserList();
    }

    @Parameterized.Parameter
    public String browserName;

    @BeforeClass
    public static void beforeAnyTestsRun() {
        driverFactory = new DriverFactory();
    }

    @Before
    public void beforeEachTestRuns() throws IOException {
        url = testRule.getUrl();

        providesDriver = driverFactory.get(true, browserName);

        providesDriver.setStubbedLocation(AcceptanceTestHelper.NearAltrincham);

        providesDriver.init();
        providesDriver.clearCookies();

        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        when = TestConfig.nextTuesday(0);
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
    public void shouldCheckNearAltrinchamToAshton() {

        String finalStation = Stations.Ashton.getName();
        String firstStation = Stations.NavigationRoad.getName();

        List<String> changes = Arrays.asList(firstStation, Stations.Piccadilly.getName());
        List<String> headSigns = Arrays.asList("", Stations.Piccadilly.getName(), finalStation);

        LocalTime time = LocalTime.parse("19:47");
        TramJourney wholeJoruney = new TramJourney(myLocation, finalStation, when, time);
        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, wholeJoruney);

        TramJourney tramJourney = new TramJourney(firstStation, finalStation, when, time);
        TramJourneyExpectations tramJourneyExpectations =
                new TramJourneyExpectations(changes, headSigns, expectedNumberJourneyResults, true);

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, tramJourney, tramJourneyExpectations, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from " + firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
        helper.checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSigns, false);

        while (journeyDetailsPage.laterTramEnabled()) {
            journeyDetailsPage.laterTram();
            helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
            // these vary depending on timing of trams, key thing is to check walking stage is first
//            checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
//            checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);
        }

    }

    @Test
    public void shouldCheckNearAltrinchamToCornbrook() {

        String firstStation = Stations.NavigationRoad.getName();
        List<String> changes = Collections.singletonList(firstStation);
        List<String> headSigns = Arrays.asList("",Stations.Piccadilly.getName());

        String finalStation = Stations.Deansgate.getName();

        LocalTime time = LocalTime.parse("19:47");
        TramJourney tramJourney = new TramJourney(myLocation, finalStation, when, time);
        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, tramJourney);

        TramJourneyExpectations expectations = new TramJourneyExpectations(changes, headSigns,
                expectedNumberJourneyResults, true);
        helper.checkDetailsAndJourneysPresent(routeDetailsPage, new TramJourney(firstStation, finalStation, when, time),
                expectations, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
    }

    @Test
    public void shouldCopeWithNearbyLocationWhenSelectingMyLocation() {

        List<String> changes = new LinkedList<>();

        String finalStation = Stations.NavigationRoad.getName();

        TramJourney tramJourney = new TramJourney(myLocation, finalStation, when,
                LocalTime.parse("19:47"));
        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, tramJourney);

        List<String> headsigns = new LinkedList<>();
        TramJourneyExpectations expectations =
                new TramJourneyExpectations(changes, headsigns, expectedNumberJourneyResults, true);

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, tramJourney, expectations, true);

    }


}
