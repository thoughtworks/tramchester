package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.graph.UnknownStationException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MediaCityTest extends JourneyPlannerHelper {
    private static Dependencies dependencies;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldFindEndOfLinesToMediaCity() throws UnknownStationException {
        for (String start : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(start, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
        }
    }

    @Test
    public void shouldFindInterchangesToMediaCity() throws UnknownStationException {
        for (String start : TransportGraphBuilder.interchanges) {
            validateAtLeastOneJourney(start, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
        }
    }

    @Test
    public void shouldFindMediaCityToEndOfLines() throws UnknownStationException {
        for (String end : Stations.EndOfTheLine) {
            validateAtLeastOneJourney(Stations.MediaCityUK, end, "08:00:00", DaysOfWeek.Monday);
        }
    }

    @Test
    public void shouldFindMediaCityToInterchanges() throws UnknownStationException {
        for (String end : TransportGraphBuilder.interchanges) {
            validateAtLeastOneJourney(Stations.MediaCityUK, end, "08:00:00", DaysOfWeek.Monday);
        }
    }

    // Same line stations to MediaCity ///////////////////////////////

    @Test
    public void shouldFindRouteHarbourCityToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.HarbourCity, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteCornbrookToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRoutePomonaToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Pomona, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    // Ashton line stations to MediaCity /////////////////////////////

    @Test
    public void shouldFindRouteEtihadToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Etihad, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteHoltTownToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.HoltTown, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldNewIslingtonToMediaCity() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.NewIslington, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldMediaCityToNewIslingtonTo() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.NewIslington, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteVeloToMediaCityAt0800() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteVeloToMediaCityAt0900() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.MediaCityUK, "09:00:00", DaysOfWeek.Monday);
    }

    // Velo to Interchanges, then Interchange to mediacity at expected travle time //////////////////////////
    @Test
    public void shouldFindRouteVeloToPicc() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Piccadily, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRoutePiccToMediaCityAtInterchangeTime() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Piccadily, Stations.MediaCityUK, "08:10:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteCornbrookToMediaCityAtInterchangeTimeForVelo() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.MediaCityUK, "08:21:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteTraffordBarMediaCityUKAtInterchangeTime() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.TraffordBar, Stations.MediaCityUK, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteMediaCityToEccles() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.MediaCityUK, Stations.Eccles, "08:35:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteHarbourCityToMediaCityAtInterchangeTime() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.HarbourCity, Stations.MediaCityUK, "08:33:00", DaysOfWeek.Monday);
    }

    // Ashton line stations to each other

    @Test
    public void shouldFindRouteAshtonToVelo() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.Ashton, Stations.VeloPark, "08:00:00", DaysOfWeek.Monday);
    }

    @Test
    public void shouldFindRouteVeloToAshton() throws UnknownStationException {
        validateAtLeastOneJourney(Stations.VeloPark, Stations.Ashton, "08:00:00", DaysOfWeek.Monday);
    }

}
