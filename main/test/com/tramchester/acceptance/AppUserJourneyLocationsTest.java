package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.TestResultSummaryRow;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.acceptance.AppUserJourneyTest.desiredJourney;
import static com.tramchester.acceptance.AppUserJourneyTest.validateAStage;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AppUserJourneyLocationsTest extends UserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";

    public static AcceptanceAppExtenstion testRule = new AcceptanceAppExtenstion(App.class, configPath);

    private final String bury = TramStations.Bury.getName();
    private final String altrincham = TramStations.Altrincham.getName();
    private final String deansgate = TramStations.Deansgate.getName();

    private LocalDate when;
    private String url;

    @BeforeAll
    static void beforeAnyTestsRun() {
        createFactory(true);
    }

    private static Stream<ProvidesDriver> getProvider() {
        return getProviderCommon();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        url = testRule.getUrl()+"/app/index.html";
        when = TestEnv.testDay();
    }

    @AfterEach
    void afterEachTestRuns(TestInfo testInfo) {
        takeScreenshotsFor(testInfo);
    }

    @AfterAll
    static void afterAllTestsRun() {
        closeFactory();
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveCorrectNearbyStops(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver);

        Assertions.assertTrue(appPage.hasLocation());
        Assertions.assertTrue(appPage.searchEnabled());

        // from
        List<String> myLocationStops = appPage.getNearbyFromStops();
        Assertions.assertEquals(1, myLocationStops.size());

        List<String> nearestFromStops = appPage.getNearestFromStops();
        assertThat("Have nearest stops", nearestFromStops, hasItems(altrincham, TramStations.NavigationRoad.getName()));

        List<String> allFrom = appPage.getAllStopsFromStops();
        assertFalse(allFrom.contains(altrincham));
        assertFalse(allFrom.contains(TramStations.NavigationRoad.getName()));

        List<String> recentFromStops = appPage.getRecentFromStops();
        assertThat(allFrom, not(contains(recentFromStops)));

        Assertions.assertEquals(TramStations.NumberOf, nearestFromStops.size() + allFrom.size() + recentFromStops.size());

        // to
        List<String> myLocationToStops = appPage.getNearbyToStops();
        Assertions.assertEquals(1, myLocationToStops.size());

        List<String> nearestToStops = appPage.getNearestFromStops();
        assertThat(nearestToStops, hasItems(altrincham, TramStations.NavigationRoad.getName()));
        List<String> allTo = appPage.getAllStopsToStops();
        assertThat(allTo, not(contains(nearestToStops)));
        int recentToCount = appPage.getRecentToStops().size();
        Assertions.assertEquals(TramStations.NumberOf, nearestToStops.size()+allTo.size()+recentToCount);

        // check recents works as expected
        desiredJourney(appPage, altrincham, bury, when, LocalTime.parse("10:15"), false);
        appPage.planAJourney();
        appPage.waitForReady();

        // set start/dest to some other stations
        appPage.setStart(TramStations.Piccadilly.getName());
        appPage.setDest(TramStations.ManAirport.getName());

        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(altrincham, bury));
        nearestFromStops = appPage.getNearestFromStops();
        assertThat(nearestFromStops, hasItems(TramStations.NavigationRoad.getName()));
        // TODO to recent just bury, not alty
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckNearAltrinchamToDeansgate(ProvidesDriver providesDriver) throws IOException {
        AppPage appPage = prepare(providesDriver);

        LocalTime planTime = LocalTime.of(10,15);
        desiredJourney(appPage, "My Location", deansgate, when, planTime, false);
        appPage.planAJourney();

        List<TestResultSummaryRow> results = appPage.getResults();
        // TODO lockdown timetable: 3 -> 2
        Assertions.assertTrue(results.size()>=2, "at least some results");

        for (TestResultSummaryRow result : results) {
            LocalTime departTime = result.getDepartTime();
            Assertions.assertTrue(departTime.isAfter(planTime), departTime.toString());

            LocalTime arriveTime = result.getArriveTime();
            Assertions.assertTrue(arriveTime.isAfter(departTime), arriveTime.toString());
            Assertions.assertEquals("Direct", result.getChanges());
        }

        // select first journey
        TestResultSummaryRow firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        Assertions.assertEquals(2, stages.size());
        Stage firstStage = stages.get(0);

        validateWalkingStage(firstStage, LocalTime.of(10,25), "Walk to",
                TramStations.Altrincham.getName(), -1, "RouteClassWalk");

        Stage secondStage = stages.get(1);
        LocalTime departTime = LocalTime.of(10,31);
        validateAStage(secondStage, departTime, "Board Tram", TramStations.Altrincham.getName(), 1,
                AppUserJourneyTest.altyToPiccClass, AppUserJourneyTest.altyToPicLineName, "Piccadilly", 9);
    }

    private void validateWalkingStage(Stage stage, LocalTime departTime, String action, String actionStation, int platform,
                                      String lineClass) {
        Assertions.assertEquals(departTime, stage.getDepartTime(), "departtime");
        Assertions.assertEquals(action, stage.getAction(), "action");
        Assertions.assertEquals(actionStation, stage.getActionStation(), "actionStation");
        Assertions.assertEquals(platform, stage.getPlatform(), "platform");

        // hidden for walking stages
        Assertions.assertEquals("", stage.getLine(lineClass), "lineName");
    }

    private AppPage prepare(ProvidesDriver providesDriver) throws IOException {
        providesDriver.setStubbedLocation(TestEnv.nearAltrincham);

        return prepare(providesDriver, url);
    }

}

