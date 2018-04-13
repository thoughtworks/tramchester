package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.*;
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
import java.util.Collections;
import java.util.LinkedList;
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
        when = JourneyPlannerHelper.nextTuesday(0);
    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldCheckNearAltrinchamToAshton() throws InterruptedException {

        String finalStation = Stations.Ashton.getName();
        String firstStation = Stations.NavigationRoad.getName();

        List<String> changes = Arrays.asList(firstStation, Stations.Piccadilly.getName());
        List<String> headSigns = Arrays.asList("",Stations.Piccadilly.getName(),finalStation);

        LocalTime time = LocalTime.parse("19:47");
        TramJourney wholeJoruney = new TramJourney(myLocation, finalStation, when, time);
        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, wholeJoruney);

        TramJourney tramJourney = new TramJourney(firstStation, finalStation, when, time);
        TramJourneyExpectations tramJourneyExpectations =
                new TramJourneyExpectations(changes, headSigns, expectedNumberJourneyResults, true);

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, tramJourney, tramJourneyExpectations, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
        helper.checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSigns, false);

        while(journeyDetailsPage.laterTramEnabled()) {
            journeyDetailsPage.laterTram();
            helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
            // these vary depending on timing of trams, key thing is to check walking stage is first
//            checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
//            checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);
        }
    }

    @Test
    public void shouldCheckNearAltrinchamToCornbrook() throws InterruptedException {

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
    public void shouldCopeWithNearbyLocationWhenSelectingMyLocation() throws InterruptedException {

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
