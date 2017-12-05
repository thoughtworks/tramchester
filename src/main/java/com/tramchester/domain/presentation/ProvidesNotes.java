package com.tramchester.domain.presentation;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;

import java.util.Collection;
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

        notes.addAll(addNotesFor(decoratedJourneys));

        return notes;
    }

    private List<String> addNotesFor(SortedSet<JourneyDTO> decoratedJourneys) {
        List<String> result = new LinkedList<>();
        decoratedJourneys.stream().forEach(journeyDTO -> {
            journeyDTO.getStages().stream().
                    filter(stageDTO -> stageDTO.getMode().equals(TransportMode.Tram)).
                    forEach(tramStage -> {
                        StationDepartureInfo info = tramStage.getPlatform().getStationDepartureInfo();
                        if (info!=null) {
                            if (!result.contains(info.getMessage())) {
                                result.add(info.getMessage());
                            }
                        }
                    });
        });
        return result;
    }
}
