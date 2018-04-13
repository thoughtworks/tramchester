package com.tramchester.acceptance.infra;

import java.util.LinkedList;
import java.util.List;

public class TramJourneyExpectations {
    public final List<String> changes;
    public final List<String> headSigns;
//    public boolean embeddedWalk; // TODO Remove?
    public int expectedJourneys;

    public TramJourneyExpectations(List<String> changes, List<String> headSigns, int expectedJourneys) {

        this.changes = changes;
        this.headSigns = headSigns;
        this.expectedJourneys = expectedJourneys;
//        embeddedWalk = false;
    }

    public TramJourneyExpectations(List<String> headSigns, int expectedJourneys) {
        this.headSigns =  headSigns;
        this.expectedJourneys = expectedJourneys;
        this.changes = new LinkedList<>();
//        embeddedWalk = false;
    }

}
