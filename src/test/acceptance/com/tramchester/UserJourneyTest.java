package com.tramchester;

import com.tramchester.pages.*;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserJourneyTest {

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, "config/localAcceptance.yml");
    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();
    private final String cornbrook = Stations.Cornbrook.getName();
    private WebDriver driver;

    private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    @Rule
    public TestName testName = new TestName();
    private LocalDate when;

    @Before
    public void beforeEachTestRuns() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT, false);
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.FINE);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);
        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
        when = LocalDate.now();
    }

    @After
    public void afterEachTestRuns() throws IOException {
        try {
            takeScreenShot();
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            logs.forEach(log -> System.out.println(log));
        } finally {
            driver.close();
        }
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
        JourneyDetailsPage journeyDetailsPage = checkJourney(altrincham, Stations.Bury.getName(),
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
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.ManAirport.getName(),
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
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.Rochdale.getName(),
                Stations.ManAirport.getName(), when, "10:15", changes,
                headSigns, false, expectedNumberJourneyResults, 0);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {
        List<String> noChanges = new LinkedList<>();
        List<String> headsSignNoChange = Arrays.asList("Piccadilly");
        List<String> headSignsBury = Arrays.asList("Bury");

        RouteDetailsPage routeDetailsPage = checkJourney(altrincham, deansgate,
                when, "10:15", noChanges, headsSignNoChange, false, expectedNumberJourneyResults, 0)
                .backToRouteDetails();
        routeDetailsPage = checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges, headsSignNoChange, 1).backToRouteDetails();
        List<String> traffordChange = Arrays.asList(Stations.TraffordBar.getName());
        checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, traffordChange, headSignsBury, 2);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Eccles");

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();

        JourneyDetailsPage journeyDetailsPage = checkJourney(ashton, piccadilly,
                when, "10:15", changes, headSigns, false, expectedNumberJourneyResults, 0);

        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        assertThat(journeyDetailsPage.getSummary(), endsWith(ashton));
        for (int index = 0; index < headSigns.size(); index++) {
            checkStage(index, journeyDetailsPage, ashton, piccadilly,
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

        checkJourney(altrincham, Stations.ExchangeSquare.getName(),
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

        RouteDetailsPage routeDetailsPage = enterRouteSelection(fromStop, toStop, when, "10:15");

        checkRoutes(fromStop, toStop, changes, true, routeDetailsPage);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        checkStage(0, journeyDetailsPage, fromStop, toStop, changes, headSigns, false);
        checkWalkingStage(1, journeyDetailsPage, fromStop, changes);
        checkStage(2, journeyDetailsPage, fromStop, toStop, changes, headSigns, true);

        MapPage mapPage = journeyDetailsPage.clickOnMapLink(1);

        assertEquals(Stations.MarketStreet.getName(), mapPage.getTitle());
    }

    private JourneyDetailsPage checkJourney(String fromStop, String toStop, LocalDate date, String time, List<String> changes,
                                            List<String> headSigns, boolean embeddedWalk, int expectedJourneys, int selectedJourney) throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(fromStop, toStop, date, time);

        return checkJourneyResultsAndDetailsFor(routeDetailsPage, fromStop, toStop, changes, headSigns, embeddedWalk, expectedJourneys, selectedJourney);
    }

    private JourneyDetailsPage checkJourneyResultsAndDetailsFor(RouteDetailsPage routeDetailsPage, String fromStop, String toStop,
                                                                List<String> changes, List<String> headSigns, boolean embeddedWalk,
                                                                int expectedJourneys, int selectedJourney) {
        checkRoutes(fromStop, toStop, changes, embeddedWalk, routeDetailsPage);

        for(int i=0;i<expectedJourneys; i++) {
            assertTrue("Check for journey "+i,routeDetailsPage.journeyPresent(i));
        }

        return checkJourneyDetailsPage(routeDetailsPage, fromStop, toStop, changes, headSigns, selectedJourney);
    }

    private JourneyDetailsPage checkJourneyDetailsPage(RouteDetailsPage routeDetailsPage, String fromStop, String toStop, List<String> changes,
                                                       List<String> headSigns, int selectedJourney) {
        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(selectedJourney);
        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));
        for (int index = 0; index < headSigns.size(); index++) {
            checkStage(index, journeyDetailsPage, fromStop, toStop, changes, headSigns, false);
        }
        return journeyDetailsPage;
    }

    private void checkRoutes(String fromStop, String toStop, List<String> changes, boolean embeddedWalk, RouteDetailsPage routeDetailsPage) {
        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String heading = routeDetailsPage.getJourneyHeading(0);
        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        String plural = (changes.size() == 1) ? "" : "s";
        if (changes.isEmpty()) {
            assertThat(heading, startsWith("Tram with No Changes - "));
        } else {
            if (embeddedWalk) {
                assertThat(heading, startsWith(format("Tram and Walk with %s change%s - ", changes.size(), plural)));
            } else {
                assertThat(heading, startsWith(format("Tram with %s change%s - ", changes.size(), plural)));
            }
        }
        assertThat(heading, endsWith(" minutes"));
        String fromStationText = " from " + fromStop;
        assertThat(begin, endsWith(fromStationText));
        assertThat(end, endsWith(" Arrive at " + toStop));
        if (changes.isEmpty()) {
            assertThat(summary, is("Direct"));
        } else {
            String summaryResult = (changes.size() == 1) ? changes.get(0) : format("%s and %s", changes.get(0), changes.get(1));
            assertThat(summary, is("Change at " + summaryResult));
        }
    }

    private RouteDetailsPage enterRouteSelection(String fromStop, String toStop, LocalDate date, String time) throws InterruptedException {
        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();

        routePlannerPage.setFromStop(fromStop);
        routePlannerPage.setToStop(toStop);
        routePlannerPage.setTime(time);
        routePlannerPage.setDate(date);
        return routePlannerPage.submit();
    }

    private void checkStage(int stageIndex, JourneyDetailsPage journeyDetailsPage, String fromStop, String toStop,
                            List<String> changes, List<String> headSigns, boolean wasWalking) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex == 0) {
            assertThat("Changes", promptText, is("Board tram at " + fromStop));
        } else if (wasWalking) {
            assertThat("Changes", promptText, is("Board tram at " + changes.get(stageIndex - 1)));
        } else {
            assertThat("Changes", promptText, is("Change tram at " + changes.get(stageIndex - 1)));
        }
        checkDuration(journeyDetailsPage, stageIndex);

        assertThat(journeyDetailsPage.getInstruction(stageIndex),
                endsWith(format("Catch %s Tram", headSigns.get(stageIndex))));
        String arriveText = journeyDetailsPage.getArrive(stageIndex);
        if (stageIndex < changes.size()) {
            assertThat(arriveText, endsWith(" Arrive at " + changes.get(stageIndex)));
        } else {
            assertThat(arriveText, endsWith(" Arrive at " + toStop));
        }
        if (stageIndex < changes.size()) {
            assertThat(journeyDetailsPage.getChange(stageIndex), is("Change Tram"));
        }
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

    private void checkDuration(JourneyDetailsPage journeyDetailsPage, int durIndex) {
        String durationText;
        durationText = journeyDetailsPage.getDuration(durIndex);
        assertThat(durationText, endsWith("min"));
        assertThat(durationText, startsWith("Duration"));
    }

    private void takeScreenShot() {
        try {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            File target = new File(format("build/reports/tests/%s.png", testName.getMethodName()));
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException e) {
            // unable to take screenshot
        }

    }

}
