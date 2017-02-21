package com.tramchester;

import com.tramchester.pages.*;
import com.tramchester.resources.FeedInfoResourceTest;
import com.tramchester.resources.JourneyPlannerHelper;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

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

public class UserJourneyTest extends UserJourneys {

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();
    private final String cornbrook = Stations.Cornbrook.getName();
    private LocalDate nextMonday;
    private String url;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();
        DesiredCapabilities capabilities = createCommonCapabilities(false);
        String firefoxPath = System.getenv("FIREFOX_PATH");
        if (firefoxPath!=null) {
            System.setProperty("webdriver.firefox.bin", firefoxPath);
        }

        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
        nextMonday = JourneyPlannerHelper.nextMonday();
    }

    @After
    public void afterEachTestRuns() throws IOException {
        commonAfter(testName);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldRedirectDirectToJourneyPageAfterFirstVisit() throws InterruptedException, UnsupportedEncodingException {
        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());
        assertTrue(welcomePage.hasBeginLink());

        assertTrue(driver.manage().getCookieNamed("tramchesterVisited")==null);
        welcomePage.begin();

        // cookie should now be set
        Cookie cookie = driver.manage().getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);

        // check redirect
        RoutePlannerPage redirectedPage = new RoutePlannerPage(driver);
        redirectedPage.load(testRule.getUrl());
        redirectedPage.waitForToStops();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToBuryThenBackToStart() throws InterruptedException {
        List<String> changes = Lists.emptyList();
        List<String> headSigns = Arrays.asList("Bury");
        JourneyDetailsPage journeyDetailsPage = checkJourney(url, altrincham, Stations.Bury.getName(),
                nextMonday, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
        // check values remembered
        assertEquals(altrincham,plannerPage.getFromStop());
        assertEquals(Stations.Bury.getName(),plannerPage.getToStop());
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
    @Category({AcceptanceTest.class})
    public void shouldHideStationInToListWhenSelectedInFromList() throws InterruptedException {
        WelcomePage welcomePage = new WelcomePage(driver);
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
    @Category({AcceptanceTest.class})
    public void shouldShowNoRoutesMessage() throws InterruptedException {
        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);
        routePlannerPage.setTime("03:00");

        RouteDetailsPage detailsPage = routePlannerPage.submit();
        assertTrue(detailsPage.waitForError());

    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAirportToDeangateThenBackToRoute() throws InterruptedException {
        List<String> changes = Lists.emptyList();
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield");
        JourneyDetailsPage journeyDetailsPage = checkJourney(url, Stations.ManAirport.getName(),
                deansgate, nextMonday, "10:15", changes,
                headSigns, false, expectedNumberJourneyResults, 0, false);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    // with second city crossing does not appear to be any three stage journeys
//    @Test
//    @Category({AcceptanceTest.class})
//    public void shouldCheckRochdaleToAirportThreeStageJourney() throws InterruptedException {
//        List<String> changes = Arrays.asList("South Chadderton", "St Werburgh's Road");
//        List<String> headSigns = Arrays.asList("Exchange Square","East Didsbury","Manchester Airport");
//        JourneyDetailsPage journeyDetailsPage = checkJourney(url, Stations.Rochdale.getName(),
//                Stations.ManAirport.getName(), nextMonday, "10:15", changes,
//                headSigns, false, expectedNumberJourneyResults, 0, false);
//        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
//        routeDetailsPage.waitForRoutes();
//    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldHaveSecondCityCrossingRoutes() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();
        List<String> headsignRochdale = Arrays.asList("Rochdale Interchange");

        checkJourney(url, Stations.StPetersSquare.getName(), Stations.ExchangeSquare.getName(),
                nextMonday, "10:15", noChanges, headsignRochdale, false, expectedNumberJourneyResults, 0, false);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();

        List<String> headsignEtihadCampus = Arrays.asList("Etihad Campus");
        List<String> headSignsBury = Arrays.asList("Bury");

        RouteDetailsPage routeDetailsPage = checkJourney(url, altrincham, deansgate,
                nextMonday, "10:15", noChanges, headSignsBury, false, expectedNumberJourneyResults, 0, false)
                .backToRouteDetails();

        routeDetailsPage = checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges,
                headsignEtihadCampus, 1).backToRouteDetails();

        checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges, headSignsBury, 2);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldDisplayNotNotesOnWeekday() throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, altrincham, deansgate, nextMonday, "10:00:00");
        assertTrue(routeDetailsPage.waitForRoutes());
        assertFalse(routeDetailsPage.notesPresent());
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldDisplayNotesOnSaturday() throws InterruptedException {
        LocalDate aSaturday = nextMonday.plusDays(5);

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, altrincham, deansgate, aSaturday, "10:00:00");
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldDisplayNotesOnSunday() throws InterruptedException {
        LocalDate aSunday = nextMonday.plusDays(6);

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, altrincham, deansgate, aSunday, "10:00:00");
        checkForWeekendNotes(routeDetailsPage);
    }

    // saving for Christmas 2017....
//    @Test
//    @Category({AcceptanceTest.class})
//    public void shouldDisplayMessageAboutChristmasServices2016() throws InterruptedException {
//        LocalDate date = new LocalDate(2016, 12, 23);
//        RouteDetailsPage page = enterRouteSelection(url, altrincham, deansgate, date, "10:00:00");
//
//        assertThat(getNotes(page),not(hasItem(ProvidesNotes.christmas)));
//
//        for(int offset=1; offset<11; offset++) {
//            RoutePlannerPage planner = page.planNewJourney();
//            LocalDate queryDate = date.plusDays(offset);
//            page = enterRouteSelection(planner, altrincham, deansgate, queryDate, "10:00:00");
//            checkForChristmasMessage(queryDate.toString(), page);
//        }
//
//        RoutePlannerPage planner = page.planNewJourney();
//        page = enterRouteSelection(planner, altrincham, deansgate, new LocalDate(2017, 1, 3), "10:00:00");
//        assertThat(getNotes(page),not(hasItem(ProvidesNotes.christmas)));
//    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Eccles");

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();

        JourneyDetailsPage journeyDetailsPage = checkJourney(url, ashton, piccadilly,
                nextMonday, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);

        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        assertThat(journeyDetailsPage.getSummary(), endsWith(ashton));
        for (int index = 0; index < headSigns.size(); index++) {
            checkStage(journeyDetailsPage, index, ashton, piccadilly,
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
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.StPetersSquare.getName());
        List<String> headSigns = Arrays.asList("Bury");

        checkJourney(url, altrincham, Stations.ExchangeSquare.getName(),
                nextMonday, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0, false);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldHaveBuildAndVersionNumberInFooter() throws InterruptedException {
        String build = System.getenv("SNAP_PIPELINE_COUNTER");
        if (build==null) {
            build = "0";
        }

        RoutePlannerPage page = new WelcomePage(driver).load(testRule.getUrl()).begin();
        String result = page.findElementById("build").getText();
        assertEquals("Build "+build,result);

        String dataBegin = page.findElementById("validFrom").getText();
        assertEquals(" From: "+ FeedInfoResourceTest.validFrom.toString("YYYY-MM-dd"), dataBegin);

        String dataEnd = page.findElementById("validUntil").getText();
        assertEquals(" Until: "+FeedInfoResourceTest.validUntil.toString("YYYY-MM-dd"), dataEnd);

    }

    @Test
    @Category({AcceptanceTest.class})
    @Ignore("Walking routes disabled now St Peters Square is open again")
    public void shouldHaveWalkingRoutesAcrossCity() throws InterruptedException {
        List<String> changes = Arrays.asList(deansgate, Stations.MarketStreet.getName());
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield", "none", "Bury");

        String fromStop = altrincham;
        String toStop = Stations.Bury.getName();

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, fromStop, toStop, nextMonday, "10:15");

        checkRoutes(routeDetailsPage, fromStop, toStop, changes, true, false, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        checkStage(journeyDetailsPage, 0, fromStop, toStop, changes, headSigns, false);
        checkWalkingStage(1, journeyDetailsPage, fromStop, changes);
        checkStage(journeyDetailsPage, 2, fromStop, toStop, changes, headSigns, true);

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
        checkDuration(journeyDetailsPage, stageIndex);
    }

    private void checkForChristmasMessage(String msg, RouteDetailsPage routeDetailsPage) {
        List<String> notes = getNotes(routeDetailsPage);
        assertThat(msg, notes,
                hasItem("There are changes to Metrolink services during Christmas and New Year.Please check TFGM for details."));
    }

    private void checkForWeekendNotes(RouteDetailsPage routeDetailsPage) {
        List<String> notes = getNotes(routeDetailsPage);
        assertThat(notes,hasItem("At the weekend your journey may be affected by improvement works.Please check TFGM for details."));
    }

    private List<String> getNotes(RouteDetailsPage routeDetailsPage) {
        return routeDetailsPage.getAllNotes();
    }

}
