package com.tramchester;

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

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
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
    public void shouldCheckAltrinchamToBury() throws InterruptedException {
        checkJourney(Stations.Altrincham.getName(), Stations.Bury.getName(), Stations.Deansgate.getName(), 1, "10:15");
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        checkJourney(Stations.Altrincham.getName(), Stations.ExchangeSquare.getName(),
                format("%s and %s",Stations.Cornbrook.getName(),  Stations.Victoria.getName()), 2, "10:15");
    }

    private void checkJourney(String fromStop, String toStop, String expectedChange, Integer changes, String time) throws InterruptedException {

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

        String plural = (changes==1) ? "" : "s";
        assertThat(heading, startsWith(format("Tram with %s change%s - ",changes, plural)));
        assertThat(heading, endsWith(" minutes"));
        assertThat(begin,endsWith(" from "+ fromStop));
        assertThat(end,endsWith(" Arrive at "+ toStop));
        assertThat(summary,is("Change at "+ expectedChange));
    }

    private void takeScreenShot()  {
        try {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            File target = new File(format("build/reports/tests/%s.png", testName.getMethodName()));
            FileOutputStream output = null;
            output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException e) {
            // unable to take screenshot
        }

    }

}
