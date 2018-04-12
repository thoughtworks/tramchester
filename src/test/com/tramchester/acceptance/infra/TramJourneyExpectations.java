package com.tramchester.acceptance.infra;

import java.util.LinkedList;
import java.util.List;

public class TramJourneyExpectations {
    public final List<String> changes;
    public final List<String> headSigns;

    public TramJourneyExpectations(List<String> changes, List<String> headSigns) {

        this.changes = changes;
        this.headSigns = headSigns;
    }

    public TramJourneyExpectations(List<String> headSigns) {
        this.headSigns =  headSigns;
        this.changes = new LinkedList<>();
    }
}
