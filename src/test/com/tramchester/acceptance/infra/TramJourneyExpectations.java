package com.tramchester.acceptance.infra;

import java.util.LinkedList;
import java.util.List;

public class TramJourneyExpectations {
    public final List<String> changes;
    public final List<String> headSigns;
    public int expectedJourneys;
    public boolean startsWithWalk;

    public TramJourneyExpectations(List<String> changes, List<String> headSigns, int expectedJourneys,
                                   boolean startsWithWalk) {

        this.changes = changes;
        this.headSigns = headSigns;
        this.expectedJourneys = expectedJourneys;
        this.startsWithWalk = startsWithWalk;
    }

    public TramJourneyExpectations(List<String> headSigns, int expectedJourneys, boolean startsWithWalk) {
        this.headSigns =  headSigns;
        this.expectedJourneys = expectedJourneys;
        this.startsWithWalk = startsWithWalk;
        this.changes = new LinkedList<>();
    }

}
