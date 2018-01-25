package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestHelper;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

@RunWith(Parameterized.class)
public class UserJourneyWithLocationTest {
    private static final String configPath = "config/localAcceptance.yml";
    private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    private String myLocation = "My Location";
    private LatLong nearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);
    private LocalDate when;
    private String url;
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList( "chrome");
    }

    @Parameterized.Parameter
    public String browserName;

    @Before
    public void beforeEachTestRuns() throws IOException {
        url = testRule.getUrl();

        providesDriver = DriverFactory.create(true, browserName);

        providesDriver.setStubbedLocation(nearAltrincham);

        providesDriver.init();
        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        when = JourneyPlannerHelper.nextMonday(0);
    }

    @After
    public void afterEachTestRuns() throws IOException {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldCheckNearAltrinchamToAshton() throws InterruptedException {

        String finalStation = "Ashton-Under-Lyne";
        String firstStation = Stations.NavigationRoad.getName();

        List<String> changes = Arrays.asList(firstStation, Stations.Piccadilly.getName());
        List<String> headSignsA = Arrays.asList("","Piccadilly",finalStation);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when,
                LocalTime.parse("19:47"));

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSignsA);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
        helper.checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);

        while(journeyDetailsPage.laterTramEnabled()) {
            journeyDetailsPage.laterTram();
            helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSignsA);
            // these vary depending on timing of trams, key thing is to check walking stage is first
//            checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
//            checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);
        }
    }

    @Test
    public void shouldCheckNearAltrinchamToCornbrook() throws InterruptedException {

        String firstStation = Stations.NavigationRoad.getName();
        List<String> changes = Arrays.asList(firstStation);
        List<String> headSigns = Arrays.asList("","Piccadilly");

        String finalStation = Stations.Deansgate.getName();

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when,
                LocalTime.parse("19:47"));

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
    }

    @Test
    public void shouldCopeWithNearbyLocationWhenSelectingMyLocation() throws InterruptedException {

        List<String> changes = Arrays.asList();

        String finalStation = Stations.NavigationRoad.getName();

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when,
                LocalTime.parse("19:47"));

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, myLocation, finalStation, changes, false,
                expectedNumberJourneyResults, true, true);

    }


}
