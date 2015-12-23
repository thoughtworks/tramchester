package com.tramchester;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserJourneyTest {

    private static final String URL = "http://localhost:8080";
//    private static App app;

    @BeforeClass
    public static void beforeAnyTestsRun() throws Exception {
//        String[] args = new String[]{"server", "config/local.yml"};
//        app = new App();
//        app.run(args);
    }

    @AfterClass
    public static void afterAllTestsHaveRun() {

    }

    @Ignore("work in progress")
    @Test
    public void shouldCheckUserJourney() throws InterruptedException {
        File profileDir = new File("src/test/acceptance/firefoxTestProfile/");

        assertTrue(profileDir.exists());

        FirefoxProfile profile = new FirefoxProfile(profileDir);
        WebDriver driver = new FirefoxDriver(profile);

        driver.get(URL);

        WebElement beginLink = driver.findElement(By.id("plan"));
        beginLink.click();

        RoutePlannerPage routePlannerPage = new RoutePlannerPage(driver);
        // todo do this better
        Thread.sleep(1000);

        routePlannerPage.setFromStop("Altrincham");
        routePlannerPage.setToStop("Bury");
        routePlannerPage.setTime("10:15");
        routePlannerPage.submit();

        RouteDetailsPage routeDetailsPage = new RouteDetailsPage(driver);
        String heading = routeDetailsPage.getJourneyHeading(0);
        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);
        assertEquals("Altrincham - Manchester - Etihad Campus Tram Line", heading);
        assertEquals("10:20 from Altrincham", begin);
        assertEquals("11:27 Arrive at Bury", end);
        assertEquals("Change at Deansgate-Castlefield", summary);

        driver.close();
    }
}
