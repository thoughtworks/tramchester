package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestHelper;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.*;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.FeedInfoResourceTest;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class UserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";
    private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    private final String bury = Stations.Bury.getName();

    @Rule
    public TestName testName = new TestName();

    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();
    private final String cornbrook = Stations.Cornbrook.getName();
    private LocalDate testingDay;
    private String url;
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    @Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList( "firefox");
    }

    @Parameterized.Parameter
    public String browserName;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();

        providesDriver = DriverFactory.create(false, browserName);
        providesDriver.init();
        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        testingDay = JourneyPlannerHelper.nextMonday(0);
    }

    @After
    public void afterEachTestRuns() throws IOException {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldRedirectDirectToJourneyPageAfterFirstVisit() throws InterruptedException, UnsupportedEncodingException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());
        assertTrue(welcomePage.hasBeginLink());

        assertTrue(providesDriver.getCookieNamed("tramchesterVisited")==null);
        welcomePage.begin();

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);

        // check redirect
        RoutePlannerPage redirectedPage = providesDriver.getRoutePlannerPage();
        redirectedPage.load(testRule.getUrl());
        redirectedPage.waitForToStops();
    }

    @Test
    public void shouldCheckAltrinchamToBuryThenBackToStart() throws InterruptedException {
        List<String> changes = Lists.emptyList();
        List<String> headSigns = Arrays.asList("Bury");
        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, altrincham, bury,
                testingDay, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
        // check values remembered
        assertEquals(altrincham,plannerPage.getFromStop());
        assertEquals(bury,plannerPage.getToStop());
        assertEquals("10:15",plannerPage.getTime());

        // check recents are set
        List<WebElement> recentFrom = plannerPage.getRecentFromStops();
        assertEquals(2, recentFrom.size());
        //
        // Need to select an element other than alty for 'from' recent stops to show in 'to'
        plannerPage.setFromStop(Stations.Ashton.getName());
        List<WebElement> recentTo = plannerPage.getRecentToStops();
        assertEquals(2, recentTo.size());
    }

    @Test
    public void shouldHideStationInToListWhenSelectedInFromList() throws InterruptedException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        List<String> toStops = routePlannerPage.getToStops();
        assertFalse(toStops.contains(altrincham));
        assertTrue(toStops.contains(cornbrook));

        routePlannerPage.setFromStop(cornbrook);
        toStops = routePlannerPage.getToStops();
        assertTrue(toStops.contains(altrincham));
        assertFalse(toStops.contains(cornbrook));
    }

    @Test
    public void shouldShowNoRoutesMessage() throws InterruptedException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);
        routePlannerPage.setTime("03:00");
        routePlannerPage.setDate(testingDay);

        RouteDetailsPage detailsPage = routePlannerPage.submit();
        assertTrue(detailsPage.waitForError());

    }

    @Test
    public void shouldCheckAirportToDeangateThenBackToRoute() throws InterruptedException {
        List<String> changes = Lists.emptyList();
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield");
        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, Stations.ManAirport.getName(),
                deansgate, testingDay, "10:15", changes,
                headSigns, false, expectedNumberJourneyResults, 0, false);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    public void shouldHaveSecondCityCrossingRoutes() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();
        List<String> headsignRochdale = Arrays.asList("Rochdale Interchange");

        helper.checkJourney(url, Stations.StPetersSquare.getName(), Stations.ExchangeSquare.getName(),
                testingDay, "10:15", noChanges, headsignRochdale, false, expectedNumberJourneyResults, 0, false);
    }

    @Test
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();

        List<String> headsignEtihadCampus = Arrays.asList("Etihad Campus");
        List<String> headSignsBury = Arrays.asList("Bury");

        RouteDetailsPage routeDetailsPage = helper.checkJourney(url, altrincham, deansgate,
                testingDay, "10:15", noChanges, headSignsBury, false, expectedNumberJourneyResults, 0, false)
                .backToRouteDetails();

        routeDetailsPage = helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges,
                headsignEtihadCampus, 1).backToRouteDetails();

        helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges, headSignsBury, 2);
    }

    @Test
    public void shouldDisplayNotNotesOnWeekday() throws InterruptedException {

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, altrincham, deansgate, testingDay, "10:00:00");
        assertTrue(routeDetailsPage.waitForRoutes());
        assertFalse(routeDetailsPage.notesPresent());
    }

    @Test
    public void shouldDisplayNotesOnSaturday() throws InterruptedException {
        LocalDate aSaturday = testingDay.plusDays(5);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, altrincham, deansgate, aSaturday, "10:00:00");
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldDisplayNotesOnSunday() throws InterruptedException {
        LocalDate aSunday = testingDay.plusDays(6);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, altrincham, deansgate, aSunday, "10:00:00");
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Eccles");

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, ashton, piccadilly,
                testingDay, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);

        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        assertThat(journeyDetailsPage.getSummary(), endsWith(ashton));
        for (int index = 0; index < headSigns.size(); index++) {
            helper.checkStage(journeyDetailsPage, index, ashton, piccadilly,
                    changes, headSigns, false);
        }

        journeyDetailsPage.laterTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertTrue(journeyDetailsPage.earlierTramEnabled());

        journeyDetailsPage.earlierTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        // cycle through all later journeys
        int count = 0;
        while(journeyDetailsPage.laterTramEnabled()) {
            LocalTime firstTime = journeyDetailsPage.getTime();
            journeyDetailsPage.laterTram();
            assertTrue(firstTime.isBefore(journeyDetailsPage.getTime()));
            count++;
        }

        // back through all earlier joruneys
        while(journeyDetailsPage.earlierTramEnabled()) {
            LocalTime beforeTime = journeyDetailsPage.getTime();
            journeyDetailsPage.earlierTram();
            assertTrue(beforeTime.isAfter(journeyDetailsPage.getTime()));
            count--;
        }

        assertEquals(0,count);
    }

    @Test
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.StPetersSquare.getName());
        List<String> headSigns = Arrays.asList("Bury");

        helper.checkJourney(url, altrincham, Stations.ExchangeSquare.getName(),
                testingDay, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);
    }

    @Test
    public void shouldHaveBuildAndVersionNumberInFooter() throws InterruptedException {
        String build = System.getenv("TRAVIS_BUILD_NUMBER");
        if (build==null) {
            build = "0";
        }

        RoutePlannerPage page = providesDriver.getWelcomePage().load(testRule.getUrl()).begin();
        String result = page.findElementById("build").getText();
        assertEquals("Build 1."+build, result);

        String dataBegin = page.getValidFrom();
        assertEquals(" From: "+ FeedInfoResourceTest.validFrom.toString("YYYY-MM-dd"), dataBegin);

        String dataEnd = page.getValidUntil();
        assertEquals(" Until: " + FeedInfoResourceTest.validUntil.toString("YYYY-MM-dd"), dataEnd);

    }

    @Test
    @Ignore("Walking routes disabled now St Peters Square is open again")
    public void shouldHaveWalkingRoutesAcrossCity() throws InterruptedException {
        List<String> changes = Arrays.asList(deansgate, Stations.MarketStreet.getName());
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield", "none", "Bury");

        String fromStop = altrincham;
        String toStop = bury;

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, fromStop, toStop, testingDay, "10:15");

        helper.checkRoutes(routeDetailsPage, fromStop, toStop, changes, true, false, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        helper.checkStage(journeyDetailsPage, 0, fromStop, toStop, changes, headSigns, false);
        checkWalkingStage(1, journeyDetailsPage, fromStop, changes);
        helper.checkStage(journeyDetailsPage, 2, fromStop, toStop, changes, headSigns, true);

        MapPage mapPage = journeyDetailsPage.clickOnMapLink(1);

        assertEquals(Stations.MarketStreet.getName(), mapPage.getTitle());
    }

    private void checkWalkingStage(int stageIndex, JourneyDetailsPage journeyDetailsPage, String fromStop,
                                   List<String> changes) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex == 0) {
            assertThat("Changes", promptText, is("Board tram at " + fromStop));
        } else {
            assertThat("Changes", promptText, is("Walk to " + changes.get(stageIndex)));
        }
        helper.checkDuration(journeyDetailsPage, stageIndex);
    }

    private void checkForWeekendNotes(RouteDetailsPage routeDetailsPage) {
        List<String> notes = getNotes(routeDetailsPage);
        assertThat(notes,hasItem("At the weekend your journey may be affected by improvement works.Please check TFGM for details."));
    }

    private List<String> getNotes(RouteDetailsPage routeDetailsPage) {
        return routeDetailsPage.getAllNotes();
    }

}
