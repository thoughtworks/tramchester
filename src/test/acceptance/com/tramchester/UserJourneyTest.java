package com.tramchester;

import com.tramchester.pages.RouteDetailsPage;
import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertTrue;

public class UserJourneyTest {

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, "config/local.yml");
    private WebDriver driver;

    @Before
    public void beforeEachTestRuns() {
        File profileDir = new File("src/test/acceptance/firefoxTestProfile/");
        assertTrue(profileDir.exists());
        FirefoxProfile profile = new FirefoxProfile(profileDir);

        driver = new FirefoxDriver(profile);
    }

    @After
    public void afterEachTestRuns() {
        //driver.close();
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckAltrinchamToBury() throws InterruptedException {
        checkJourney("Altrincham", "Bury", "Deansgate-Castlefield", 1, "10:15");
    }

    private void checkJourney(String fromStop, String toStop, String expectedChange, Integer changes, String time) throws InterruptedException {

        WelcomePage welcomePage = new WelcomePage(driver);
        welcomePage.load(testRule.getUrl());

        welcomePage.begin();

        RoutePlannerPage routePlannerPage = new RoutePlannerPage(driver);
        routePlannerPage.waitForToStops();

        routePlannerPage.setFromStop(fromStop);
        routePlannerPage.setToStop(toStop);
        routePlannerPage.setTime(time);
        routePlannerPage.submit();

        RouteDetailsPage routeDetailsPage = new RouteDetailsPage(driver);
        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String heading = routeDetailsPage.getJourneyHeading(0);
        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        assertThat(heading, startsWith("Tram with "+changes+" change - "));
        assertThat(heading, endsWith(" minutes"));
        assertThat(begin,endsWith(" from "+ fromStop));
        assertThat(end,endsWith(" Arrive at "+ toStop));
        assertThat(summary,is("Change at "+ expectedChange));
    }

}
