package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertTrue;

public class AcceptanceTestHelper {

    public static LatLong NearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);

    private ProvidesDriver driver;

    public AcceptanceTestHelper(ProvidesDriver driver) {
        this.driver = driver;
    }

    public JourneyDetailsPage checkJourney(String url, TramJourney tramJourney,
                                              TramJourneyExpectations tramJourneyExpectations,
                                              int selectedJourney, boolean onlyWalk) throws InterruptedException {

        RouteDetailsPage routeDetailsPage = enterRouteSelection(url, tramJourney);
        checkDetailsAndJourneysPresent(routeDetailsPage, tramJourney, tramJourneyExpectations, onlyWalk);
        return checkJourneyDetailsPage(routeDetailsPage, tramJourney.fromStop, tramJourney.toStop,
                tramJourneyExpectations.changes, tramJourneyExpectations.headSigns, selectedJourney);

    }

    public void checkDetailsAndJourneysPresent(RouteDetailsPage routeDetailsPage, TramJourney tramJourney,
                                               TramJourneyExpectations expectations, boolean onlyWalk) {
        checkRoutes(routeDetailsPage, tramJourney.fromStop, tramJourney.toStop, expectations.changes,
                expectations.startsWithWalk, onlyWalk);

        for(int i=0;i<expectations.expectedJourneys; i++) {
            assertTrue("Check for journey "+i,routeDetailsPage.journeyPresent(i));
        }

    }

    public JourneyDetailsPage checkJourneyDetailsPage(RouteDetailsPage routeDetailsPage, String fromStop, String toStop, List<String> changes,
                                                         List<String> headSigns, int selectedJourney) {
        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(selectedJourney);
        String fromStationText = " from " + fromStop;
        assertThat(journeyDetailsPage.getSummary(), endsWith(fromStationText));
        for (int index = 0; index < headSigns.size(); index++) {
            checkStage(journeyDetailsPage, index, fromStop, toStop, changes, headSigns, false);
        }
        return journeyDetailsPage;
    }

    private void checkRoutes(RouteDetailsPage routeDetailsPage, String fromStop, String toStop, List<String> changes,
                             boolean startsWithWalk, boolean onlyWalk) {
        assertTrue(routeDetailsPage.waitForRoutes());
        assertTrue(routeDetailsPage.journeyPresent(0));

        String begin = routeDetailsPage.getJourneyBegin(0);
        String end = routeDetailsPage.getJourneyEnd(0);
        String summary = routeDetailsPage.getSummary(0);

        checkHeading(routeDetailsPage, changes, startsWithWalk, onlyWalk);

        String fromStationText = " from " + fromStop;
        assertThat(begin, endsWith(fromStationText));
        assertThat(end, endsWith(" Arrive at " + toStop));

        if (noChanges(changes, startsWithWalk)) {
            assertThat(summary, is("Direct"));
        } else {
            String summaryResult = (changes.size() == 1) ? changes.get(0) : format("%s and %s", changes.get(0), changes.get(1));
            assertThat(summary, is("Change at " + summaryResult));
        }
    }

    private void checkHeading(RouteDetailsPage routeDetailsPage, List<String> changes,
                              boolean startsWithWalk, boolean onlyWalk) {
        String heading = routeDetailsPage.getJourneyHeading(0);

        String plural = (changes.size() == 1) ? "" : "s";

        String expectedHeading;
        if (onlyWalk) {
            expectedHeading = "Walk with No Changes -";
        } else {
            if (noChanges(changes, startsWithWalk)) {
                expectedHeading = "Tram with No Changes - ";
            } else {
                expectedHeading = format("Tram with %s change%s - ", changes.size(), plural);
            }
            if (startsWithWalk) {
                expectedHeading = "Walk and " + expectedHeading;
            }
        }
        assertThat(heading, startsWith(expectedHeading));
        assertThat(heading, endsWith(" minutes"));
    }

    private boolean noChanges(List<String> changes, boolean startsWithWalk) {
        int numChanges = changes.size();
        if (startsWithWalk) {
            numChanges--;
        }
        return numChanges<=0;
    }

    public RouteDetailsPage enterRouteSelection(String url, TramJourney tramJourney) {
        WelcomePage welcomePage = driver.getWelcomePage();
        welcomePage.load(url);
        RoutePlannerPage routePlannerPage = welcomePage.begin();
        return enterRouteSelection(routePlannerPage, tramJourney);
    }

    private RouteDetailsPage enterRouteSelection(RoutePlannerPage routePlannerPage, TramJourney tramJourney) {
        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(tramJourney.fromStop);
        routePlannerPage.setToStop(tramJourney.toStop);
        routePlannerPage.setTime(tramJourney.time);
        routePlannerPage.setDate(tramJourney.date);

        return routePlannerPage.submitJourney();
    }

    public void checkStage(JourneyDetailsPage journeyDetailsPage, int stageIndex, String fromStop, String toStop,
                              List<String> changes, List<String> headSigns, boolean previousStageWasWalk) {
        String promptText = journeyDetailsPage.getPrompt(stageIndex);
        if (stageIndex == 0) {
            if (previousStageWasWalk) {
                assertThat("Changes", promptText, is("Walk to " + fromStop));
            } else {
                assertThat("Changes", promptText, is("Board tram at " + fromStop));
            }
        } else if (previousStageWasWalk) {
            assertThat("Changes", promptText, is("Board tram at " + changes.get(stageIndex - 1)));
        } else {
            assertThat("Changes", promptText, is("Change tram at " + changes.get(stageIndex - 1)));
        }
        checkDuration(journeyDetailsPage, stageIndex);
        if (!headSigns.isEmpty()) {
            checkInstruction(journeyDetailsPage, stageIndex, headSigns);
        }

        String arriveText = journeyDetailsPage.getArrive(stageIndex);
        if (stageIndex < changes.size()) {
            assertThat(arriveText, endsWith(" Arrive at " + changes.get(stageIndex)));
        } else {
            assertThat(arriveText, endsWith(" Arrive at " + toStop));
        }
        if (stageIndex < changes.size()) {
            assertThat(journeyDetailsPage.getChange(stageIndex), is("Change Tram"));
        }
    }

    public void checkInitialWalkingStage(JourneyDetailsPage journeyDetailsPage, String fromStop,
                                            List<String> headSigns) {

        assertThat("Changes", journeyDetailsPage.getPrompt(0), is("Walk to " + fromStop));

        checkDuration(journeyDetailsPage, 0);
        checkInstruction(journeyDetailsPage, 0, headSigns);

        assertThat(journeyDetailsPage.getArrive(0), is(""));
        assertThat(journeyDetailsPage.getChange(0), is(""));
    }

    private void checkInstruction(JourneyDetailsPage journeyDetailsPage, int stageIndex, List<String> headSigns) {
        String expectedHeadsign = headSigns.get(stageIndex);
        String instruction = journeyDetailsPage.getInstruction(stageIndex);
        if (expectedHeadsign.isEmpty()) {
            assertTrue(instruction.isEmpty());
        }
        else {
            assertTrue(format("%s not in %s ",expectedHeadsign, instruction),
                    instruction.contains(format("Catch %s Tram", expectedHeadsign)));
        }
    }

    private void checkDuration(JourneyDetailsPage journeyDetailsPage, int durIndex) {
        String durationText;
        durationText = journeyDetailsPage.getDuration(durIndex);
        assertThat(durationText, endsWith("min"));
        assertThat(durationText, startsWith("Duration"));
    }
}
