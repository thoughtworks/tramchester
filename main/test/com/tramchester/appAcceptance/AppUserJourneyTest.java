package com.tramchester.appAcceptance;

import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.acceptance.infra.*;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.SummaryResult;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.FeedInfoResourceTest;
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

import static com.tramchester.TestConfig.dateFormatDashes;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class AppUserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";
    private static DriverFactory driverFactory;

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    private final String bury = Stations.Bury.getName();
    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();

    @Rule
    public TestName testName = new TestName();

    private LocalDate nextTuesday;
    private String url;
    private ProvidesDriver providesDriver;

    public static List<String> getBrowserList() {
        if (System.getenv("CIRCLECI") == null) {
            return Arrays.asList("chrome", "firefox");
        }
        // TODO - confirm this is still an issue
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

    @BeforeClass
    public static void beforeAnyTestsRun() {
        driverFactory = new DriverFactory();
    }

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl()+"/app/index.html";

        providesDriver = driverFactory.get(false, browserName);
        providesDriver.init();
        providesDriver.clearCookies();

        // TODO offset for when tfgm data is expiring
        nextTuesday = TestConfig.nextTuesday(0);
    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @AfterClass
    public static void afterAllTestsRun() {
        driverFactory.close();
        driverFactory.quit();
    }

    @Test
    public void shouldHaveInitialValuesAndSetInputsSetCorrectly() {
        AppPage appPage = prepare();

        LocalTime now = LocalTime.now();
        String timeOnPageRaw = appPage.getTime();
        LocalTime timeOnPage = LocalTime.parse(timeOnPageRaw);

        int diff = Math.abs(now.toSecondOfDay() - timeOnPage.toSecondOfDay());
        assertTrue(diff<=60); // allow for page render and webdriver overheads

        assertEquals(LocalDate.now(), appPage.getDate());

        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
        assertJourney(appPage, altrincham, bury, "10:15", nextTuesday);
        desiredJourney(appPage, altrincham, bury, nextTuesday.plusMonths(1), LocalTime.parse("03:15"));
        assertJourney(appPage, altrincham, bury, "03:15", nextTuesday.plusMonths(1));
        desiredJourney(appPage, altrincham, bury, nextTuesday.plusYears(1), LocalTime.parse("20:15"));
        assertJourney(appPage, altrincham, bury, "20:15", nextTuesday.plusYears(1));
    }

    @Test
    public void shouldTravelAltyToBuryAndSetRecents() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        // check recents are set
        List<String> fromRecent = appPage.getRecentFromStops();
        assertTrue(fromRecent.contains(altrincham));
        assertTrue(fromRecent.contains(bury));
        // check "non recents" don't contain these two stations now
        List<String> remainingStops = appPage.getAllStopsFromStops();
        assertFalse(remainingStops.contains(altrincham));
        assertFalse(remainingStops.contains(bury));
        // still displaying all stations
        assertEquals(93, remainingStops.size()+fromRecent.size());

        // inputs still set
        assertJourney(appPage, altrincham, bury, "10:15", nextTuesday);
    }

    @Test
    public void shouldCheckAltrinchamToDeansgate() {
        AppPage appPage = prepare();
        LocalTime planTime = LocalTime.parse("10:15");
        desiredJourney(appPage, altrincham, deansgate, nextTuesday, planTime);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        assertEquals(6, results.size());

        for (SummaryResult result : results) {
            assertTrue(result.getDepartTime().isAfter(planTime));
            assertTrue(result.getArriveTime().isAfter(result.getDepartTime()));
            assertEquals("Direct", result.getChanges());
            assertEquals("Tram with No Changes - 23 minutes", result.getSummary());
        }

        // select first journey
        SummaryResult firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(1, stages.size());
        Stage stage = stages.get(0);
        validateAStage(stage, firstResult.getDepartTime(), "Board tram at", altrincham, 1,
                "Altrincham - Manchester - Bury Tram line", "RouteClass1", Stations.Bury.getName(), 9);
    }

    @Test
    public void shouldHideStationInToListWhenSelectedInFromList() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
        List<String> toStops = appPage.getToStops();
        assertFalse(toStops.contains(altrincham));
    }

    @Test
    public void shouldShowNoRoutesMessage() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("03:15"));
        appPage.planAJourney();

        assertTrue(appPage.noResults());
    }

    @Test
    public void shouldUpdateWhenNewJourneyIsEntered() {
        LocalTime tenFifteen = LocalTime.parse("10:15");
        LocalTime eightFifteen = LocalTime.parse("08:15");

        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, tenFifteen);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isAfter(tenFifteen));

        desiredJourney(appPage, altrincham, bury, nextTuesday, eightFifteen);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isBefore(tenFifteen));
        assertTrue(results.get(0).getDepartTime().isAfter(eightFifteen));

    }

    @Test
    public void shouldHaveMultistageJourney() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, Stations.ManAirport.getName(), nextTuesday, LocalTime.parse("10:15"));
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        assertEquals(6, results.size());


        // select first journey
        SummaryResult firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());

        Stage firstStage = stages.get(0);
        Stage secondStage = stages.get(1);

        validateAStage(firstStage, firstResult.getDepartTime(), "Board tram at", altrincham, 1,
                "Altrincham - Manchester - Bury Tram line", "RouteClass1",
                Stations.Bury.getName(), 7);
        validateAStage(secondStage, LocalTime.parse("10:48"), "Change tram at", Stations.TraffordBar.getName(),
                2, "Victoria - Manchester Airport Tram line", "RouteClass6",
                Stations.ManAirport.getName(), 17);

        assertEquals(Stations.TraffordBar.getName(), secondStage.getActionStation());
        assertEquals("Change tram at", secondStage.getAction());

    }

    @Test
    public void shouldDisplayWeekendWorkNoteOnlyOnWeekends() {
        LocalTime time = LocalTime.parse("10:15");
        String weekendMsg = "At the weekend your journey may be affected by improvement works.Please check TFGM for details.";

        AppPage appPage = prepare();
        LocalDate aSaturday = TestConfig.nextSaturday();

        desiredJourney(appPage, altrincham, bury, aSaturday, time);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertThat(appPage.getAllNotes(), hasItem(weekendMsg));

        desiredJourney(appPage, altrincham, bury, nextTuesday, time);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertFalse(appPage.notesPresent());

        desiredJourney(appPage, altrincham, bury, aSaturday.plusDays(1), time);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertThat(appPage.getAllNotes(), hasItem(weekendMsg));
    }

    @Test
    public void shouldHaveBuildAndVersionNumberInFooter() {
        AppPage appPage = prepare();

        String build = appPage.getExpectedBuildNumberFromEnv();

        String result = appPage.getBuild();
        assertEquals("2."+build, result);

        String dataBegin = appPage.getValidFrom();
        assertEquals(FeedInfoResourceTest.validFrom.format(dateFormatDashes), dataBegin);

        String dataEnd = appPage.getValidUntil();
        assertEquals(FeedInfoResourceTest.validUntil.format(dateFormatDashes), dataEnd);

    }

    @Test
    public void shouldDisplayCookieAgreementIfNotVisited() throws UnsupportedEncodingException {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertNull(providesDriver.getCookieNamed("tramchesterVisited"));

        assertTrue(appPage.waitForCookieAgreementVisible());

        appPage.agreeToCookies();

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);
        assertTrue(appPage.waitForCookieAgreementInvisible());

        AppPage afterReload = providesDriver.getAppPage();
        assertTrue(afterReload.waitForCookieAgreementInvisible());
        afterReload.waitForToStops();
    }

    @Test
    public void shouldDisplayDisclaimer() {
        AppPage appPage = prepare();

        assertFalse(appPage.waitForDisclaimerVisible());

        appPage.displayDisclaimer();
        assertTrue(appPage.waitForDisclaimerVisible());

        appPage.dismissDisclaimer();
        // chrome takes a while to close it, so wait for it to go
        assertTrue(appPage.waitForDisclaimerInvisible());
    }

    private AppPage prepare() {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);
        appPage.waitForCookieAgreementVisible();
        appPage.agreeToCookies();
        appPage.waitForCookieAgreementInvisible();
        appPage.waitForToStops();
        return appPage;
    }

    private void desiredJourney(AppPage appPage, String start, String dest, LocalDate date, LocalTime time) {
        appPage.setStart(start);
        appPage.setDest(dest);
        appPage.setDate(date);
        appPage.setTime(time);
    }

    private void assertJourney(AppPage appPage, String start, String dest, String time, LocalDate date) {
        assertEquals(start, appPage.getFromStop());
        assertEquals(dest, appPage.getToStop());
        assertEquals(time, appPage.getTime());
        assertEquals(date, appPage.getDate());
    }

    private void validateAStage(Stage stage, LocalTime departTime, String action, String actionStation, int platform,
                                String lineName, String lineClass, String headsign, int stops) {
        assertEquals(departTime, stage.getDepartTime());
        assertEquals(action, stage.getAction());
        assertEquals(actionStation, stage.getActionStation());
        assertEquals(platform, stage.getPlatform());
        assertEquals(headsign, stage.getHeadsign());
        assertEquals(lineName, stage.getLine(lineClass));
        assertEquals(stops, stage.getPassedStops());
    }

}

