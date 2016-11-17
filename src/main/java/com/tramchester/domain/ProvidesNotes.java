package com.tramchester.domain;

import java.util.LinkedList;
import java.util.List;

public class ProvidesNotes {
    private String weekend = "At the weekend your journey may be affected by improvement works." +
            "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";

    public List<String> createNotesFor(TramServiceDate queryDate) {
        List<String> notes = new LinkedList<>();
        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }
        return notes;
    }
}
