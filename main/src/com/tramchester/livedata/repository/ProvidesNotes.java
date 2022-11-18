package com.tramchester.livedata.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Journey;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.tfgm.ProvidesTramNotes;

import java.util.List;

@ImplementedBy(ProvidesTramNotes.class)
public interface ProvidesNotes {
    List<Note> createNotesForJourney(Journey journey, TramDate queryDate);

    List<Note> createNotesForStations(List<Station> stations, TramDate queryDate, TramTime time);
}
