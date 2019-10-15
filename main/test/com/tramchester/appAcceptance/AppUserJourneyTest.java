package com.tramchester.appAcceptance;

import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.acceptance.infra.*;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Cookie;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class AppUserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    private final String bury = Stations.Bury.getName();
    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();
    private final String cornbrook = Stations.Cornbrook.getName();

    @Rule
    public TestName testName = new TestName();

    private LocalDate nextTuesday;
    private String url;
    private ProvidesDriver providesDriver;

    public static List<String> getBrowserList() {
        if (System.getenv("CIRCLECI") == null) {
            return Arrays.asList("chrome", "firefox");
        }
        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
        return Arrays.asList("firefox");
    }

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
        return getBrowserList();
    }

    @Parameterized.Parameter
    public String browserName;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl()+"/app/index.html";

        providesDriver = DriverFactory.create(false, browserName);
        providesDriver.init();

        // TODO offset for when tfgm data is expiring
        nextTuesday = TestConfig.nextTuesday(0);
    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldRedirectDirectToJourneyPageAfterFirstVisit() throws UnsupportedEncodingException {
        AppPage welcomePage = providesDriver.getAppPage();
        welcomePage.load(url);
        assertTrue(welcomePage.hasConsentButton());

        assertNull(providesDriver.getCookieNamed("tramchesterVisited"));
        welcomePage.consent();

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);

        // check redirect
        AppPage shouldBeRedirected = providesDriver.getAppPage();
        shouldBeRedirected.load(url);
        shouldBeRedirected.waitForToStops();
    }

    @Test
    public void shouldInputsSetCorrectly() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));

        // check values set as expected
        assertEquals(altrincham, appPage.getFromStop());
        assertEquals(bury, appPage.getToStop());
        assertEquals("10:15", appPage.getTime());
        assertEquals(nextTuesday, appPage.getDate());
    }

    @Test
    public void shouldTravelAltyToBuryAndSetRecents() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
        appPage.planAJourney();
        assertTrue(appPage.resultsSelectable());

        // check recents are set
        List<String> fromRecent = appPage.getRecentFromStops();
        assertTrue(fromRecent.contains(altrincham));
        assertTrue(fromRecent.contains(bury));
    }

    private AppPage prepare() {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);
        appPage.consent();
        appPage.waitForToStops();
        return appPage;
    }

    private void desiredJourney(AppPage appPage, String start, String dest, LocalDate date, LocalTime time) {
        appPage.setStart(start);
        appPage.setDest(dest);
        appPage.setDate(date);
        appPage.setTime(time);
    }

}

