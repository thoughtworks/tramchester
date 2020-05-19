package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.SummaryResult;
import com.tramchester.testSupport.Stations;
import com.tramchester.integration.resources.FeedInfoResourceTest;
import com.tramchester.testSupport.TestEnv;
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
import java.util.Collections;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class AppUserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";
    private static DriverFactory driverFactory;

    // NOTE: Needs correct locale settings, see .circleci/config.yml setupLocale target

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

    // useful consts, keep around as can swap when timetable changes
    private static final String altyToBuryClass = "RouteClass1";
    public static final String altyToPiccClass = "RouteClass2";
    private static final String altyToBuryLineName = "Altrincham - Manchester - Bury";
    public static final String altyToPicLineName = "Altrincham - Piccadilly";

    private static List<String> getBrowserList() {
        if (!TestEnv.isCircleci()) {
            return Arrays.asList("chrome", "firefox");
        }

        // TODO - confirm this is still an issue
        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
        return Collections.singletonList("firefox");
    }

    @Parameterized.Parameters(name="{index}: {0}")
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
        nextTuesday = TestEnv.nextTuesday(0);
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
    public void shouldShowInitialCookieConsentAndThenDismiss() {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue(appPage.waitForCookieAgreementVisible());
        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible());
        assertTrue(appPage.waitForReady());
        assertTrue(appPage.waitForToStops());
    }

    @Test
    public void shouldHaveInitialValuesAndSetInputsSetCorrectly() {
        AppPage appPage = prepare();

        validateCurrentTimeIsSelected(appPage);

        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());

        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"), false);
        assertJourney(appPage, altrincham, bury, "10:15", nextTuesday, false);
        desiredJourney(appPage, altrincham, bury, nextTuesday.plusMonths(1), LocalTime.parse("03:15"), false);
        assertJourney(appPage, altrincham, bury, "03:15", nextTuesday.plusMonths(1), false);

        appPage.selectNow();
        validateCurrentTimeIsSelected(appPage);

        appPage.selectToday();
        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());
    }

    private void validateCurrentTimeIsSelected(AppPage appPage) {
        LocalTime timeOnPage = timeSelectedOnPage(appPage);
        LocalTime now = TestEnv.LocalNow().toLocalTime();
        int diff = Math.abs(now.toSecondOfDay() - timeOnPage.toSecondOfDay());
        // allow for page render and webdriver overheads
        assertTrue(String.format("now:%s timeOnPage: %s diff: %s", now, timeOnPage, diff), diff <= 110);
    }

    private LocalTime timeSelectedOnPage(AppPage appPage) {
        String timeOnPageRaw = appPage.getTime();
        return LocalTime.parse(timeOnPageRaw);
    }

    @Test
    public void shouldTravelAltyToBuryAndSetRecents() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"), false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        // so above station in recents
        appPage.setStart(Stations.ExchangeSquare.getName()); // so alty is available in the recents list
        appPage.setDest(Stations.PiccadillyGardens.getName()); // so alty is available in the recents list

        // check 'from' recents are set
        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(altrincham, bury));
        List<String> remainingFromStops = appPage.getAllStopsFromStops();
        assertThat(remainingFromStops, not(contains(fromRecent)));
        // still displaying all stations
        assertEquals(Stations.NumberOf-1, remainingFromStops.size()+fromRecent.size()); // less one as 'to' stop is excluded

        // check 'to' recents are set
        List<String> toRecent = appPage.getRecentToStops();
        assertThat(toRecent, hasItems(altrincham,bury));
        List<String> remainingToStops = appPage.getAllStopsToStops();
        assertThat(remainingToStops, not(contains(toRecent)));
        assertEquals(Stations.NumberOf-1, remainingToStops.size()+toRecent.size()); // less one as 'from' stop is excluded

        // inputs still set
        assertJourney(appPage, Stations.ExchangeSquare.getName(), Stations.PiccadillyGardens.getName(), "10:15", nextTuesday, false);
    }

    @Test
    public void shouldCheckAltrinchamToDeansgate() {
        AppPage appPage = prepare();
        LocalTime planTime = LocalTime.parse("10:00");
        desiredJourney(appPage, altrincham, deansgate, nextTuesday, planTime, false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        // TODO Lockdown 3->2
        assertTrue("at least 2 results", results.size()>=2);

        LocalTime previous = planTime;
        for (SummaryResult result : results) {
            assertTrue(result.getDepartTime().isAfter(previous));
            assertTrue(result.getArriveTime().isAfter(result.getDepartTime()));
            assertEquals("Direct", result.getChanges());
            previous = result.getDepartTime();
        }

        // select first journey
        SummaryResult firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(1, stages.size());
        Stage stage = stages.get(0);

        validateAStage(stage, firstResult.getDepartTime(), "Board", altrincham, 1,
                altyToPiccClass, altyToPicLineName, Stations.Piccadilly.getName(), 9);
    }

    @Test
    public void shouldHideStationInToListWhenSelectedInFromList() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("10:15"), false);

        appPage.waitForToStopsContains(Stations.Bury);
        List<String> destStops = appPage.getToStops();
        assertFalse("should not contain alty", destStops.contains(altrincham));
    }

    @Test
    public void shouldShowNoRoutesMessage() {
        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, LocalTime.parse("03:15"), false);
        appPage.planAJourney();

        assertTrue(appPage.noResults());
    }

    @Test
    public void shouldUpdateWhenNewJourneyIsEntered() {
        LocalTime tenFifteen = LocalTime.parse("10:15");
        LocalTime eightFifteen = LocalTime.parse("08:15");

        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isAfter(tenFifteen));

        desiredJourney(appPage, altrincham, bury, nextTuesday, eightFifteen, false);
        appPage.planAJourney();
        // need way to delay response for this test to be useful
        //assertFalse(appPage.searchEnabled());
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> updatedResults = appPage.getResults();
        assertTrue(updatedResults.get(0).getDepartTime().isBefore(tenFifteen));
        assertTrue(updatedResults.get(0).getDepartTime().isAfter(eightFifteen));
    }

    @Test
    public void shouldUpdateWhenEarlierClicked() {
        LocalTime tenFifteen = LocalTime.parse("10:15");

        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> results = appPage.getResults();
        LocalTime firstDepartureTime = results.get(0).getDepartTime();
        assertTrue(firstDepartureTime.isAfter(tenFifteen));

        appPage.earlier();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> updatedResults = appPage.getResults();
        assertTrue(updatedResults.get(0).getDepartTime().isBefore(firstDepartureTime));
    }

    @Test
    public void shouldUpdateWhenLaterClicked() {
        LocalTime tenFifteen = LocalTime.parse("10:15");

        AppPage appPage = prepare();
        desiredJourney(appPage, altrincham, bury, nextTuesday, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isAfter(tenFifteen));
        LocalTime lastDepartureTime = results.get(results.size() - 1).getDepartTime();

        appPage.later();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<SummaryResult> updatedResults = appPage.getResults();
        assertTrue(updatedResults.get(0).getDepartTime().isAfter(lastDepartureTime));
    }

    @Test
    public void shouldHaveMultistageJourney() {
        AppPage appPage = prepare();
        LocalTime planTime = LocalTime.parse("10:00");
        desiredJourney(appPage, altrincham, Stations.ManAirport.getName(), nextTuesday, planTime, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        List<SummaryResult> results = appPage.getResults();
        // TODO pre-lockdown timetable was 3
        assertTrue("at least 2 journeys, was "+results.size(),results.size()>=2);
        LocalTime previousArrivalTime = planTime; // sorted by arrival time, so we may seen
        for (SummaryResult result : results) {
            LocalTime arriveTime = result.getArriveTime();
            assertTrue(arriveTime.isAfter(result.getDepartTime()));
            assertTrue(arriveTime.isAfter(previousArrivalTime) || arriveTime.equals(previousArrivalTime));
            assertEquals(result.getChanges(), Stations.TraffordBar.getName());
            previousArrivalTime = arriveTime;
        }

        // select first journey
        SummaryResult firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());

        Stage firstStage = stages.get(0);
        Stage secondStage = stages.get(1);

        validateAStage(firstStage, firstResult.getDepartTime(), "Board", altrincham, 1,
                altyToPiccClass, altyToPicLineName,
                Stations.Piccadilly.getName(), 7);
        // Too timetable dependent?
        validateAStage(secondStage, LocalTime.parse("10:32"), "Change", Stations.TraffordBar.getName(),
                2, "RouteClass6", "Victoria - Manchester Airport",
                Stations.ManAirport.getName(), 17);

        assertEquals(Stations.TraffordBar.getName(), secondStage.getActionStation());
        assertEquals("Change", secondStage.getAction());

    }

    @Test
    public void shouldDisplayWeekendWorkNoteOnlyOnWeekends() {
        LocalTime time = LocalTime.parse("10:15");
        String weekendMsg = "At the weekend your journey may be affected by improvement works.Please check TFGM for details.";

        AppPage appPage = prepare();
        LocalDate aSaturday = TestEnv.nextSaturday();

        desiredJourney(appPage, altrincham, bury, aSaturday, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());
        assertThat(appPage.getAllNotes(), hasItem(weekendMsg));

        desiredJourney(appPage, altrincham, bury, nextTuesday, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.noWeekendMessage());
//        if (appPage.notesPresent()) {
//            assertThat(appPage.getAllNotes(), not(hasItem(weekendMsg)));
//        }

        desiredJourney(appPage, altrincham, bury, aSaturday.plusDays(1), time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());
        assertThat(appPage.getAllNotes(), hasItem(weekendMsg));
    }

    @Test
    public void shouldHaveBuildAndVersionNumberInFooter() {
        AppPage appPage = prepare();

        String build = appPage.getExpectedBuildNumberFromEnv();

        String result = appPage.getBuild();
        assertEquals("2."+build, result);

        String dataBegin = appPage.getValidFrom();
        assertEquals("valid from", FeedInfoResourceTest.validFrom.format(dateFormatDashes), dataBegin);

        String dataEnd = appPage.getValidUntil();
        assertEquals("valid until", FeedInfoResourceTest.validUntil.format(dateFormatDashes), dataEnd);

    }

    @Test
    public void shouldDisplayCookieAgreementIfNotVisited() throws UnsupportedEncodingException {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertNull(providesDriver.getCookieNamed("tramchesterVisited"));

        assertTrue(appPage.waitForCookieAgreementVisible());

        appPage.agreeToCookies();
        assertTrue("wait for cookie agreement to close", appPage.waitForCookieAgreementInvisible());

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        assertNotNull("cookie null", cookie);
        assertNotNull("cookie null", cookie.getValue());
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

        appPage.displayDisclaimer();
        assertTrue(appPage.waitForDisclaimerVisible());

        appPage.dismissDisclaimer();
        // chrome takes a while to close it, so wait for it to go
        assertTrue(appPage.waitForDisclaimerInvisible());
    }

    private AppPage prepare() {
        return prepare(providesDriver, url);
    }

    public static AppPage prepare(ProvidesDriver providesDriver, String url) {
        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue("cookie agreement visible", appPage.waitForCookieAgreementVisible());
        appPage.agreeToCookies();
        assertTrue("wait for cookie agreement to close", appPage.waitForCookieAgreementInvisible());
        assertTrue("app ready", appPage.waitForReady());
        assertTrue("stops appeared", appPage.waitForToStops());

        return appPage;
    }

    public static void desiredJourney(AppPage appPage, String start, String dest, LocalDate date, LocalTime time, boolean arriveBy) {
        appPage.setStart(start);
        appPage.setDest(dest);
        appPage.setSpecificDate(date);
        appPage.setTime(time);
        appPage.setArriveBy(arriveBy);
    }

    private static void assertJourney(AppPage appPage, String start, String dest, String time, LocalDate date, boolean arriveBy) {
        assertEquals(start, appPage.getFromStop());
        assertEquals(dest, appPage.getToStop());
        assertEquals(time, appPage.getTime());
        assertEquals(date, appPage.getDate());
        assertEquals(arriveBy, appPage.getArriveBy());
    }

    public static void validateAStage(Stage stage, LocalTime departTime, String action, String actionStation, int platform,
                                      String lineClass, String lineName, String headsign, int passedStops) {
        assertEquals("departTime", departTime, stage.getDepartTime());
        assertEquals("action", action, stage.getAction());
        assertEquals("actionStation", actionStation, stage.getActionStation());
        assertEquals("platform", platform, stage.getPlatform());
        assertEquals("lineName", lineName, stage.getLine(lineClass));
        assertEquals("headsign", headsign, stage.getHeadsign());
        assertEquals("passedStops", passedStops, stage.getPassedStops());
    }

}

