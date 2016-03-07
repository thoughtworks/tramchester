package com.tramchester;

import com.tramchester.pages.JourneyDetailsPage;
import com.tramchester.pages.RouteDetailsPage;
import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserJourneyTest {

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, "config/local.yml");
    private WebDriver driver;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void beforeEachTestRuns() {
        File profileDir = new File("src/test/acceptance/firefoxTestProfile/");
        assertTrue(profileDir.exists());
        FirefoxProfile profile = new FirefoxProfile(profileDir);

        driver = new FirefoxDriver(profile);
    }

    @After
    public void afterEachTestRuns() throws IOException {
        takeScreenShot();
        driver.close();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToBuryThenBackToStart() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Deansgate.getName());
        List<String> headSigns = Arrays.asList("Etihad Campus", "Bury");
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.Altrincham.getName(), Stations.Bury.getName(), changes,
                "10:15", headSigns);
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
    public void shouldCheckAirportToBuryThenBackToRoute() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.TraffordBar.getName());
        List<String> headSigns = Arrays.asList("Cornbrook","Bury");
        JourneyDetailsPage journeyDetailsPage = checkJourney(Stations.ManAirport.getName(), Stations.Bury.getName(), changes, "10:15",
                headSigns);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Cornbrook.getName(),  Stations.Victoria.getName());
        List<String> headSigns = Arrays.asList("Etihad Campus");

        checkJourney(Stations.Altrincham.getName(), Stations.ExchangeSquare.getName(),
               changes, "10:15", headSigns);
    }

    private JourneyDetailsPage checkJourney(String fromStop, String toStop, List<String> changes,
                                            String time, List<String> headSigns) throws InterruptedException {

        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();

        routePlannerPage.setFromStop(fromStop);
        routePlannerPage.setToStop(toStop);
        routePlannerPage.setTime(time);
        RouteDetailsPage routeDetailsPage = routePlannerPage.submit();

        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String heading = routeDetailsPage.getJourneyHeading(0);
        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        String plural = (changes.size()==1) ? "" : "s";
        assertThat(heading, startsWith(format("Tram with %s change%s - ",changes.size(), plural)));
        assertThat(heading, endsWith(" minutes"));
        String fromStationText = " from " + fromStop;
        assertThat(begin,endsWith(fromStationText));
        assertThat(end,endsWith(" Arrive at "+ toStop));
        String summaryResult = (changes.size()==1) ? changes.get(0) : format("%s and %s", changes.get(0), changes.get(1));
        assertThat(summary,is("Change at "+ summaryResult));

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);

        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));

        for(int index=0; index<headSigns.size(); index++) {
            checkStage(index, journeyDetailsPage, fromStop, toStop, changes, headSigns);
        }
        return journeyDetailsPage;
    }

    private void checkStage(int stageIndex, JourneyDetailsPage journeyDetailsPage, String fromStop, String toStop, List<String> changes, List<String> headSigns) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex==0) {
            assertThat(promptText, is("Board tram at " + fromStop));
        } else {
            assertThat(promptText, is("Change tram at " +changes.get(stageIndex-1)));
        }
        assertThat(journeyDetailsPage.getInstruction(stageIndex), endsWith(format("Catch %s Tram", headSigns.get(stageIndex))));
        checkDuration(journeyDetailsPage, stageIndex);
        String arriveText = journeyDetailsPage.getArrive(stageIndex);
        if (stageIndex<changes.size()) {
            assertThat(arriveText, endsWith(" Arrive at " + changes.get(stageIndex)));
        } else {
            assertThat(arriveText, endsWith(" Arrive at " +toStop));
        }
        if (stageIndex<changes.size()) {
            assertThat(journeyDetailsPage.getChange(stageIndex), is("Change Tram"));
        }
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
