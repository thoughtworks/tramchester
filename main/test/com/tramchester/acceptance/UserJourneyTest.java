package com.tramchester.acceptance;

import com.google.common.collect.ImmutableList;
import com.tramchester.App;
import com.tramchester.TestConfig;
import com.tramchester.acceptance.infra.*;
import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.FeedInfoResourceTest;
import com.tramchester.integration.resources.JourneyPlannerHelper;

import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.TestConfig.dateFormatDashes;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.*;

 @RunWith(Parameterized.class)
 public class UserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";
     private static DriverFactory driverFactory;
     private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

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
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    public static List<String> getBrowserList() {
        if (System.getenv("CIRCLECI")==null) {
            return Arrays.asList( "chrome", "firefox");
        }
        // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
        // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
        return Arrays.asList("firefox");
    }

    @Parameters
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
        url = testRule.getUrl();

        providesDriver = driverFactory.get(false, browserName);
        providesDriver.init();
        providesDriver.clearCookies();
        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        nextTuesday = TestConfig.nextTuesday(0);
    }

     @AfterClass
     public static void afterAllTestsRun() {
         driverFactory.close();
         driverFactory.quit();
     }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldRedirectDirectToJourneyPageAfterFirstVisit() throws UnsupportedEncodingException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());
        assertTrue(welcomePage.hasBeginLink());

        assertTrue(providesDriver.getCookieNamed("tramchesterVisited")==null);
        welcomePage.begin();

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);

        // check redirect
        RoutePlannerPage redirectedPage = providesDriver.getRoutePlannerPage();
        redirectedPage.load(testRule.getUrl());
        redirectedPage.waitForToStops();
    }

    @Test
    public void shouldCheckAltrinchamToBuryThenBackToStart() {
        List<String> headSigns = Collections.singletonList(bury);

        TramJourney tramJourney = new TramJourney(altrincham, bury, nextTuesday, LocalTime.parse("10:15"));

        TramJourneyExpectations expectations = new TramJourneyExpectations(headSigns, expectedNumberJourneyResults, false);

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney, expectations, 0, false);

        providesDriver.clearCookies();
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
        // check values remembered
        assertEquals(altrincham, plannerPage.getFromStop());
        assertEquals(bury, plannerPage.getToStop());
        assertEquals("10:15", plannerPage.getTime());

        // check recents are set
        List<String> recentFrom = plannerPage.getRecentFromStops().stream().
                map(element -> element.getText()).collect(Collectors.toList());
        assertTrue(recentFrom.contains(altrincham));
        assertTrue(recentFrom.contains(bury));

        // Need to select an element other than alty for 'from' recent stops to show in 'to'
        plannerPage.setFromStop(Stations.Ashton.getName());
        List<String> recentTo = plannerPage.getRecentToStops().stream().
                map(element -> element.getText()).collect(Collectors.toList());
        assertTrue(recentTo.contains(altrincham));
        assertTrue(recentTo.contains(bury));
    }

    @Test
    public void shouldHideStationInToListWhenSelectedInFromList() {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        List<String> toStops = routePlannerPage.getToStops();
        assertFalse(toStops.contains(altrincham));
        assertTrue(toStops.contains(cornbrook));

        routePlannerPage.setFromStop(cornbrook);
        toStops = routePlannerPage.getToStops();
        assertTrue(toStops.contains(altrincham));
        assertFalse(toStops.contains(cornbrook));
    }

    @Test
    public void shouldShowNoRoutesMessage() {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        LocalTime time = LocalTime.of(3,0);

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);
        routePlannerPage.setTime(time);
        routePlannerPage.setDate(nextTuesday);

        RouteDetailsPage detailsPage = routePlannerPage.submitJourney();
        assertTrue(detailsPage.waitForError());

    }

    @Test
    public void shouldSetDateAndTimeCorrectly() {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);

        LocalTime timeA = LocalTime.parse("03:55");
        LocalTime timeB = LocalTime.parse("21:45");

        routePlannerPage.setTime(timeA);
        LocalTime setTime = LocalTime.parse(routePlannerPage.getTime());
        assertEquals(timeA, setTime);

        routePlannerPage.setTime(timeB);
        setTime = LocalTime.parse(routePlannerPage.getTime());
        assertEquals(timeB, setTime);

        // date should initially be set to today
        LocalDate setDate = LocalDate.parse(routePlannerPage.getDate());
        assertEquals(LocalDate.now(),setDate);

        routePlannerPage.setDate(nextTuesday);
        setDate = LocalDate.parse(routePlannerPage.getDate());
        assertEquals(nextTuesday,setDate);

    }

    @Test
    public void shouldCheckAirportToDeangateThenBackToRoute() {
        List<String> headSigns = Collections.singletonList(Stations.Victoria.getName());
        TramJourney tramJourney = new TramJourney(Stations.ManAirport.getName(), deansgate, nextTuesday, LocalTime.of(10,15));
        TramJourneyExpectations tramJourneyExpectations = new TramJourneyExpectations(headSigns, expectedNumberJourneyResults, false);

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney,
                tramJourneyExpectations, 0, false);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    public void shouldHaveSecondCityCrossingRoutes() {
        List<String> headsignRochdale = ImmutableList.of(Stations.ShawAndCrompton.getName());

        TramJourney tramJourney = new TramJourney(Stations.StPetersSquare.getName(), Stations.ExchangeSquare.getName(),
                nextTuesday, LocalTime.parse("10:15"));
        TramJourneyExpectations tramJourneyExpectations = new TramJourneyExpectations(headsignRochdale, expectedNumberJourneyResults, false);

        JourneyDetailsPage detailsPage = helper.checkJourney(url, tramJourney, tramJourneyExpectations, 0, false);
        String instruction = detailsPage.getInstruction(0);
        assertTrue(instruction.contains("Tram from Platform"));

    }

    @Test
    public void shouldCheckAltrinchamToDeansgate() {

        List<String> headsignEtihadCampus = Collections.singletonList(Stations.Piccadilly.getName());
        List<String> headSignsBury = Collections.singletonList(bury);

        TramJourney tramJourney = new TramJourney(altrincham, deansgate, nextTuesday, LocalTime.parse("10:15"));

        RouteDetailsPage routeDetailsPage = helper.checkJourney(url, tramJourney,
                new TramJourneyExpectations(headSignsBury, expectedNumberJourneyResults, false), 0, false)
                .backToRouteDetails();

        List<String> noChanges = new LinkedList<>();
        routeDetailsPage = helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges,
                headsignEtihadCampus, 1).backToRouteDetails();

        helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges, headSignsBury, 2);
    }

    @Test
    public void shouldNotDisplayNotesOnWeekday() throws InterruptedException {

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, nextTuesday,
                LocalTime.parse("10:00")));
        assertTrue(routeDetailsPage.waitForRoutes());
        assertFalse(routeDetailsPage.notesPresent());
    }

    @Test
    public void shouldDisplayNotesOnSaturday() {
        LocalDate aSaturday = TestConfig.nextSaturday(); //nextTuesday.minusDays(3);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, aSaturday,
                LocalTime.parse("10:00")));
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldDisplayNotesOnSunday() {
        LocalDate aSunday = TestConfig.nextSunday();

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, aSunday,
                LocalTime.parse("10:00")));
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldCheckAshtonToPiccadilyGardens() {
        List<String> noChanges = new LinkedList<>();
        List<String> headSigns = Collections.singletonList(Stations.Eccles.getName());

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();
        TramJourney tramJourney = new TramJourney(ashton, piccadilly, nextTuesday, LocalTime.parse("10:15"));

        TramJourneyExpectations tramJourneyExpectations = new TramJourneyExpectations(noChanges, headSigns,
                expectedNumberJourneyResults, false);
        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney, tramJourneyExpectations, 0, false);

        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        assertThat(journeyDetailsPage.getSummary(), endsWith(ashton));
        for (int index = 0; index < headSigns.size(); index++) {
            helper.checkStage(journeyDetailsPage, index, ashton, piccadilly,
                    noChanges, headSigns, false);
        }

        journeyDetailsPage.laterTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertTrue(journeyDetailsPage.earlierTramEnabled());

        journeyDetailsPage.earlierTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        // cycle through all later journeys
        int count = 0;
        while(journeyDetailsPage.laterTramEnabled()) {
            LocalTime firstTime = journeyDetailsPage.getTime();
            journeyDetailsPage.laterTram();
            assertTrue(firstTime.isBefore(journeyDetailsPage.getTime()));
            count++;
        }

        // back through all earlier joruneys
        while(journeyDetailsPage.earlierTramEnabled()) {
            LocalTime beforeTime = journeyDetailsPage.getTime();
            journeyDetailsPage.earlierTram();
            assertTrue(beforeTime.isAfter(journeyDetailsPage.getTime()));
            count--;
        }

        assertEquals(0,count);
    }

    @Test
    public void shouldCheckAltrinchamToExchangeSquare() {
        List<String> changes = Collections.singletonList(Stations.Deansgate.getName());
        List<String> headSigns = Collections.singletonList(bury);

        TramJourney tramJourney = new TramJourney(altrincham, Stations.ExchangeSquare.getName(), nextTuesday,
                LocalTime.parse("10:15"));
        TramJourneyExpectations expectations =
                new TramJourneyExpectations(changes, headSigns, expectedNumberJourneyResults, false);

        helper.checkJourney(url, tramJourney, expectations, 0, false);
    }

    @Test
    public void shouldHaveBuildAndVersionNumberInFooter() {
        String build = selectBuildNumber();

        RoutePlannerPage page = providesDriver.getWelcomePage().load(testRule.getUrl()).begin();

        String result = page.getBuild();
        assertEquals("Build 2."+build, result);

        String dataBegin = page.getValidFrom();
        assertEquals(" From: "+ FeedInfoResourceTest.validFrom.format(dateFormatDashes), dataBegin);

        String dataEnd = page.getValidUntil();
        assertEquals(" Until: " + FeedInfoResourceTest.validUntil.format(dateFormatDashes), dataEnd);
    }

    private void checkForWeekendNotes(RouteDetailsPage routeDetailsPage) {
        List<String> notes = getNotes(routeDetailsPage);
        assertThat(notes,hasItem("At the weekend your journey may be affected by improvement works.Please check TFGM for details."));
    }

    private List<String> getNotes(RouteDetailsPage routeDetailsPage) {
        return routeDetailsPage.getAllNotes();
    }

    private String selectBuildNumber() {
        String build = System.getenv("CIRCLE_BUILD_NUM");
        if (build==null) {
            build = "0";
        }
        return build;
    }

}
