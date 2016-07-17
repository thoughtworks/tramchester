package com.tramchester;

import com.tramchester.pages.*;
import org.joda.time.LocalDate;
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
    private WebDriver driver;

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
        when = new LocalDate(2016, 6, 27);
    }

    @After
    public void afterEachTestRuns() throws IOException {
        try {
            takeScreenShot();
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            logs.forEach(log -> System.out.println(log));
        }
        finally {
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
    @Ignore("summer 2016 closure")
    public void shouldCheckAltrinchamToBuryThenBackToStart() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Deansgate.getName());
        List<String> headSigns = Arrays.asList("Etihad Campus", "Bury");
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.Altrincham.getName(), Stations.Bury.getName(),
                when, "10:15", changes, headSigns, false);
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToPiccadiltNotPossibleDuringClosures() throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(Stations.Altrincham.getName(),
                Stations.Piccadilly.getName(), when, "10:15");

        assertTrue(routeDetailsPage.waitForError());
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldHideStationInToListWhenSelectedInFromList() throws InterruptedException {
        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(Stations.Altrincham.getName());
        List<String> toStops = routePlannerPage.getToStops();
        assertFalse(toStops.contains(Stations.Altrincham.getName()));
        assertTrue(toStops.contains(Stations.Cornbrook.getName()));

        routePlannerPage.setFromStop(Stations.Cornbrook.getName());
        toStops = routePlannerPage.getToStops();
        assertTrue(toStops.contains(Stations.Altrincham.getName()));
        assertFalse(toStops.contains(Stations.Cornbrook.getName()));
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAirportToDeangateThenBackToRoute() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Cornbrook.getName());
        List<String> headSigns = Arrays.asList("Cornbrook");
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.ManAirport.getName(),
                Stations.Deansgate.getName(), when, "10:15", changes,
                headSigns, false);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield");

        checkJourney(Stations.Altrincham.getName(), Stations.Deansgate.getName(),
                when, "10:15", changes, headSigns, false);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Bury");

        checkJourney(Stations.Ashton.getName(), Stations.PiccadillyGardens.getName(),
                when, "10:15", changes, headSigns, false);
    }

    @Test
    @Category({AcceptanceTest.class})
    @Ignore("summer 2016 closure")
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Cornbrook.getName(),  Stations.Victoria.getName());
        List<String> headSigns = Arrays.asList("Etihad Campus");

        checkJourney(Stations.Altrincham.getName(), Stations.ExchangeSquare.getName(),
                when, "10:15", changes, headSigns, false);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldHaveWalkingRoutesAcrossCity() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Deansgate.getName(),  Stations.MarketStreet.getName());
        List<String> headSigns = Arrays.asList("Deansgate-Castlefield", "none", "Bury");

        String fromStop = Stations.Altrincham.getName();
        String toStop = Stations.Bury.getName();

        RouteDetailsPage routeDetailsPage = enterRouteSelection(fromStop, toStop, when, "10:15");

        String fromStationText = checkRoutes(fromStop, toStop, changes, true, routeDetailsPage);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        checkStage(0, journeyDetailsPage, fromStop, toStop, changes, headSigns, false);
        checkWalkingStage(1, journeyDetailsPage, fromStop, changes);
        checkStage(2, journeyDetailsPage, fromStop, toStop, changes, headSigns, true);

        MapPage mapPage = journeyDetailsPage.clickOnMapLink(1);

        assertEquals(Stations.MarketStreet.getName(),mapPage.getTitle());
    }

    private JourneyDetailsPage checkJourney(String fromStop, String toStop, LocalDate date, String time, List<String> changes,
                                            List<String> headSigns, boolean embeddedWalk) throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(fromStop, toStop, date, time);

        String fromStationText = checkRoutes(fromStop, toStop, changes, embeddedWalk, routeDetailsPage);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        for(int index=0; index<headSigns.size(); index++) {
            checkStage(index, journeyDetailsPage, fromStop, toStop, changes, headSigns, false);
        }
        return journeyDetailsPage;
    }

    private String checkRoutes(String fromStop, String toStop, List<String> changes, boolean embeddedWalk, RouteDetailsPage routeDetailsPage) {
        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String heading = routeDetailsPage.getJourneyHeading(0);
        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        String plural = (changes.size()==1) ? "" : "s";
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
        assertThat(begin,endsWith(fromStationText));
        assertThat(end,endsWith(" Arrive at "+ toStop));
        if (changes.isEmpty()) {
            assertThat(summary, is("Direct"));
        } else {
            String summaryResult = (changes.size() == 1) ? changes.get(0) : format("%s and %s", changes.get(0), changes.get(1));
            assertThat(summary, is("Change at " + summaryResult));
        }
        return fromStationText;
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
        if (stageIndex==0) {
            assertThat("Changes", promptText, is("Board tram at " + fromStop));
        } else if (wasWalking) {
            assertThat("Changes", promptText, is("Board tram at " + changes.get(stageIndex - 1)));
        }
        else {
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
        if (stageIndex<changes.size()) {
            assertThat(journeyDetailsPage.getChange(stageIndex), is("Change Tram"));
        }
    }

    private void checkWalkingStage(int stageIndex, JourneyDetailsPage journeyDetailsPage, String fromStop,
                                   List<String> changes) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex==0) {
            assertThat("Changes", promptText, is("Board tram at " + fromStop));
        } else {
            assertThat("Changes", promptText, is("Walk to " +changes.get(stageIndex)));
        }
        checkDuration(journeyDetailsPage, stageIndex);
    }

    private void checkDuration(JourneyDetailsPage journeyDetailsPage, int durIndex) {
        String durationText;
        durationText = journeyDetailsPage.getDuration(durIndex);
        assertThat(durationText, endsWith("min"));
        assertThat(durationText, startsWith("Duration"));
    }

    private void takeScreenShot()  {
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
