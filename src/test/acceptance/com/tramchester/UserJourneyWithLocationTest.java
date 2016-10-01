package com.tramchester;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.pages.JourneyDetailsPage;
import com.tramchester.pages.RouteDetailsPage;
import com.tramchester.resources.JourneyPlannerHelper;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class UserJourneyWithLocationTest extends UserJourneys {

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    private Path path = Paths.get("geofile.json");
    private String myLocation = "My Location";
    private LatLong nearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);
    private LocalDate when;
    private String url;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();

        DesiredCapabilities capabilities = createCommonCapabilities(true);

        createGeoFile();

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("geo.prompt.testing", true);
        profile.setPreference("geo.prompt.testing.allow", true);
        profile.setPreference("geo.wifi.uri", "file://" + path.toAbsolutePath().toString());
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);

        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
        when = JourneyPlannerHelper.nextMonday();
    }

    @After
    public void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(path);
        commonAfter(testName);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckNearAltrinchamToAshton() throws InterruptedException {

        assertTrue(Files.exists(path));

        String finalStation = "Ashton-Under-Lyne";
        String firstStation = Stations.NavigationRoad.getName();

        List<String> changes = Arrays.asList(firstStation, Stations.PiccadillyGardens.getName());
        List<String> headSigns = Arrays.asList("","Piccadilly",finalStation);

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
        checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSigns, false);
        while(journeyDetailsPage.laterTramEnabled()) {
            journeyDetailsPage.laterTram();
            checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
            checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
            checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSigns, false);
        }
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckNearAltrinchamToCornbrook() throws InterruptedException {

        assertTrue(Files.exists(path));

        String firstStation = Stations.NavigationRoad.getName();
        List<String> changes = Arrays.asList(firstStation);
        List<String> headSigns = Arrays.asList("","Piccadilly");

        String finalStation = Stations.Deansgate.getName();

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCopeWithNearbyLocationWhenSelectingMyLocation() throws InterruptedException {
        assertTrue(Files.exists(path));

        String firstStation = Stations.NavigationRoad.getName();
        List<String> changes = Arrays.asList(firstStation);

        String finalStation = Stations.NavigationRoad.getName();

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true);

    }

    private void createGeoFile() {
        String json = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"accuracy\": 10.0,\n" +
                "    \"location\": {\n" +
                "        \"lat\": " +nearAltrincham.getLat() + ",\n" +
                "        \"lng\": " +nearAltrincham.getLon()+"\n" +
                "     }\n" +
                "}";

        try {
            FileUtils.writeStringToFile(path.toFile(), json);
        } catch (IOException e) {
            // this is asserted later
        }
    }
}
