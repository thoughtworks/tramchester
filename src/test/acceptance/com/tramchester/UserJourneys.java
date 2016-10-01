package com.tramchester;

import com.tramchester.pages.JourneyDetailsPage;
import com.tramchester.pages.RouteDetailsPage;
import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.joda.time.LocalDate;
import org.junit.rules.TestName;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertTrue;


public class UserJourneys {
    protected WebDriver driver;
    protected static final String configPath = "config/localAcceptance.yml";
    protected int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    protected void takeScreenShot(TestName testName) {
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

    protected void commonAfter(TestName testName) {
        try {
            takeScreenShot(testName);
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            logs.forEach(log -> System.out.println(log));
        } finally {
            driver.close();
        }
    }

    protected DesiredCapabilities createCommonCapabilities(boolean enableGeo) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT, enableGeo);
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.FINE);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);
        return capabilities;
    }

    protected JourneyDetailsPage checkJourney(String url, String fromStop, String toStop, LocalDate date, String time,
                                              List<String> changes,
                                              List<String> headSigns, boolean embeddedWalk, int expectedJourneys,
                                              int selectedJourney, boolean onlyWalk) throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, fromStop, toStop, date, time);
        checkDetailsAndJourneysPresent(routeDetailsPage, fromStop, toStop, changes, embeddedWalk, expectedJourneys, false, onlyWalk);
        return checkJourneyDetailsPage(routeDetailsPage, fromStop, toStop, changes, headSigns, selectedJourney);

    }

    protected void checkDetailsAndJourneysPresent(RouteDetailsPage routeDetailsPage, String fromStop, String toStop,
                                                  List<String> changes, boolean embeddedWalk,
                                                  int expectedJourneys, boolean startsWithWalk, boolean onlyWalk) {
        checkRoutes(routeDetailsPage, fromStop, toStop, changes, embeddedWalk, startsWithWalk, onlyWalk);

        for(int i=0;i<expectedJourneys; i++) {
            assertTrue("Check for journey "+i,routeDetailsPage.journeyPresent(i));
        }

    }

    protected JourneyDetailsPage checkJourneyDetailsPage(RouteDetailsPage routeDetailsPage, String fromStop, String toStop, List<String> changes,
                                                         List<String> headSigns, int selectedJourney) {
        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(selectedJourney);
        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));
        for (int index = 0; index < headSigns.size(); index++) {
            checkStage(journeyDetailsPage, index, fromStop, toStop, changes, headSigns, false);
        }
        return journeyDetailsPage;
    }

    protected void checkRoutes(RouteDetailsPage routeDetailsPage, String fromStop, String toStop, List<String> changes,
                               boolean embeddedWalk, boolean startsWithWalk, boolean onlyWalk) {
        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        checkHeading(routeDetailsPage, changes, embeddedWalk, startsWithWalk, onlyWalk);

        String fromStationText = " from " + fromStop;
        assertThat(begin, endsWith(fromStationText));
        assertThat(end, endsWith(" Arrive at " + toStop));

        if (noChanges(changes, startsWithWalk)) {
            assertThat(summary, is("Direct"));
        } else {
            String summaryResult = (changes.size() == 1) ? changes.get(0) : format("%s and %s", changes.get(0), changes.get(1));
            assertThat(summary, is("Change at " + summaryResult));
        }
    }

    private void checkHeading(RouteDetailsPage routeDetailsPage, List<String> changes, boolean embeddedWalk,
                              boolean startsWithWalk, boolean onlyWalk) {
        String heading = routeDetailsPage.getJourneyHeading(0);

        String plural = (changes.size() == 1) ? "" : "s";

        String expectedHeading;
        if (onlyWalk) {
            expectedHeading = "Walk with No Changes -";
        } else {
            if (noChanges(changes, startsWithWalk)) {
                expectedHeading = "Tram with No Changes - ";
            } else {
                if (embeddedWalk) {
                    expectedHeading = format("Tram and Walk with %s change%s - ", changes.size(), plural);
                } else {
                    expectedHeading = format("Tram with %s change%s - ", changes.size(), plural);
                }
            }
            if (startsWithWalk && !embeddedWalk) {
                expectedHeading = "Walk and " + expectedHeading;
            }
        }
        assertThat(heading, startsWith(expectedHeading));
        assertThat(heading, endsWith(" minutes"));
    }

    private boolean noChanges(List<String> changes, boolean startsWithWalk) {
        int numChanges = changes.size();
        if (startsWithWalk) {
            numChanges--;
        }
        return numChanges<=0;
    }

    protected RouteDetailsPage enterRouteSelection(String url, String fromStop, String toStop, LocalDate date, String time) throws InterruptedException {
        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(url);

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();

        routePlannerPage.setFromStop(fromStop);
        routePlannerPage.setToStop(toStop);
        routePlannerPage.setTime(time);
        routePlannerPage.setDate(date);
        return routePlannerPage.submit();
    }

    protected void checkStage(JourneyDetailsPage journeyDetailsPage, int stageIndex, String fromStop, String toStop,
                              List<String> changes, List<String> headSigns, boolean previousStageWasWalk) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex == 0) {
            if (previousStageWasWalk) {
                assertThat("Changes", promptText, is("Walk to " + fromStop));
            } else {
                assertThat("Changes", promptText, is("Board tram at " + fromStop));
            }
        } else if (previousStageWasWalk) {
            assertThat("Changes", promptText, is("Board tram at " + changes.get(stageIndex - 1)));
        } else {
            assertThat("Changes", promptText, is("Change tram at " + changes.get(stageIndex - 1)));
        }
        checkDuration(journeyDetailsPage, stageIndex);
        checkInstruction(journeyDetailsPage, stageIndex, headSigns);

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

    protected void checkInitialWalkingStage(JourneyDetailsPage journeyDetailsPage, String fromStop,
                                            List<String> headSigns) {

        assertThat("Changes", journeyDetailsPage.getPrompt(0), is("Walk to " + fromStop));

        checkDuration(journeyDetailsPage, 0);
        checkInstruction(journeyDetailsPage, 0, headSigns);

        assertThat(journeyDetailsPage.getArrive(0), is(""));
        assertThat(journeyDetailsPage.getChange(0), is(""));
    }

    private void checkInstruction(JourneyDetailsPage journeyDetailsPage, int stageIndex, List<String> headSigns) {
        String expectedHeadsign = headSigns.get(stageIndex);
        if (expectedHeadsign.isEmpty()) {
            assertTrue(journeyDetailsPage.getInstruction(stageIndex).isEmpty());
        }
        else {
            assertThat(journeyDetailsPage.getInstruction(stageIndex),
                    endsWith(format("Catch %s Tram", expectedHeadsign)));
        }
    }

    protected void checkDuration(JourneyDetailsPage journeyDetailsPage, int durIndex) {
        String durationText;
        durationText = journeyDetailsPage.getDuration(durIndex);
        assertThat(durationText, endsWith("min"));
        assertThat(durationText, startsWith("Duration"));
    }
}
