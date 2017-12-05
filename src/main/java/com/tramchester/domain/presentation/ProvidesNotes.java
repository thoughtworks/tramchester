package com.tramchester.domain.presentation;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

public class ProvidesNotes {

    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";

    public static String weekend = "At the weekend your journey may be affected by improvement works." + website;

    public static String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;

    public List<String> createNotesFor(TramServiceDate queryDate, SortedSet<JourneyDTO> decoratedJourneys) {
        List<String> notes = new LinkedList<>();

        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }

        if (queryDate.isChristmasPeriod()) {
            notes.add(christmas);
        }

        return notes;
    }
}
