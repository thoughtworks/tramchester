package com.tramchester;

import com.tramchester.pages.*;
import com.tramchester.resources.JourneyPlannerHelper;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    private LocalDate when;
    private String url;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();
        DesiredCapabilities capabilities = createCommonCapabilities(false);

        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
        when = JourneyPlannerHelper.nextMonday();
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

        // check cookie now present
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
                when, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0);
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
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
    public void shouldCheckAirportToDeangateThenBackToRoute() throws InterruptedException {
        List<String> changes = Arrays.asList(cornbrook);
        List<String> headSigns = Arrays.asList("Cornbrook");
        JourneyDetailsPage journeyDetailsPage = checkJourney(url, Stations.ManAirport.getName(),
                deansgate, when, "10:15", changes,
                headSigns, false, expectedNumberJourneyResults, 0);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckRochdaleToAirportThreeStageJourney() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Victoria.getName(), "St Werburgh's Road");
        List<String> headSigns = Arrays.asList("Exchange Square","East Didsbury","Manchester Airport");
        JourneyDetailsPage journeyDetailsPage = checkJourney(url, Stations.Rochdale.getName(),
                Stations.ManAirport.getName(), when, "10:15", changes,
                headSigns, false, expectedNumberJourneyResults, 0);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();
        //List<String> deansgate = Arrays.asList(Stations.Deansgate.getName());

        List<String> headsignPiccadilly = Arrays.asList("Piccadilly");
        List<String> headSignsBury = Arrays.asList("Bury");

        RouteDetailsPage routeDetailsPage = checkJourney(url, altrincham, this.deansgate,
                when, "10:15", noChanges, headsignPiccadilly, false, expectedNumberJourneyResults, 0)
                .backToRouteDetails();

        routeDetailsPage = checkJourneyDetailsPage(routeDetailsPage, altrincham, this.deansgate, noChanges, headSignsBury, 1)
                .backToRouteDetails();

        checkJourneyDetailsPage(routeDetailsPage, altrincham, this.deansgate, noChanges, headsignPiccadilly, 2);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Eccles");

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();

        JourneyDetailsPage journeyDetailsPage = checkJourney(url, ashton, piccadilly,
                when, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0);

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

        int count = 0;
        while(journeyDetailsPage.laterTramEnabled()) {
            LocalTime firstTime = journeyDetailsPage.getTime();
            journeyDetailsPage.laterTram();
            assertTrue(firstTime.isBefore(journeyDetailsPage.getTime()));
            count++;
        }

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
        List<String> changes = Arrays.asList(Stations.Victoria.getName());
        List<String> headSigns = Arrays.asList("Bury");

        checkJourney(url, altrincham, Stations.ExchangeSquare.getName(),
                when, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0);
    }

    @Test
    @Category({AcceptanceTest.class})
    @Ignore("Walking routes disabled now St Peters Square is open again")
    public void shouldHaveWalkingRoutesAcrossCity() throws InterruptedException {
        List<String> changes = Arrays.asList(deansgate, Stations.MarketStreet.getName());
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield", "none", "Bury");

        String fromStop = altrincham;
        String toStop = Stations.Bury.getName();

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, fromStop, toStop, when, "10:15");

        checkRoutes(routeDetailsPage, fromStop, toStop, changes, true, false);

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


}
