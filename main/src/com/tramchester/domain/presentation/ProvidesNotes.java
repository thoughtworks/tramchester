package com.tramchester.domain.presentation;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.HasPlatformMessage;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.LiveDataRepository;
import org.apache.commons.collections4.map.HashedMap;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.presentation.Note.NoteType.*;
import static java.lang.String.format;

public class ProvidesNotes {
    private static final String EMPTY = "<no message>";
    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";
    public static String weekend = "At the weekend your journey may be affected by improvement works." + website;
    public static String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;
    private static final int MESSAGE_LIFETIME = 5;

    private final TramchesterConfig config;
    private final LiveDataRepository liveDataRepository;

    public ProvidesNotes(TramchesterConfig config, LiveDataRepository liveDataRepository) {
        this.config = config;
        this.liveDataRepository = liveDataRepository;
    }

    public <T extends CallsAtPlatforms> List<Note> createNotesForJourneys(Set<T> journeys, TramServiceDate queryDate) {
        List<Note> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(liveNotesForJourneys(journeys, queryDate));
        return notes;
    }

    public List<Note> createNotesForStations(List<Station> stations, TramServiceDate queryDate, TramTime time) {
        List<Note> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(createLiveNotesForStations(stations, queryDate, time));
        return notes;
    }

    private List<Note> createLiveNotesForStations(List<Station> stations, TramServiceDate date, TramTime time) {
        // Map: Note -> Location
        Map<Note, String> messageMap = new HashedMap<>();

        stations.forEach(station -> liveDataRepository.departuresFor(station, date, time).forEach(info ->
                addRelevantNote(messageMap, info)));

        return createMessageList(messageMap);
    }

    private List<Note> createNotesForADate(TramServiceDate queryDate) {
        ArrayList<Note> notes = new ArrayList<>();
        if (queryDate.isWeekend()) {
            notes.add(new Note(Weekend, weekend));
        }
        if (queryDate.isChristmasPeriod()) {
            notes.add(new Note(Christmas, christmas));
        }
        notes.addAll(createNotesForClosedStations());
        return notes;
    }

    private Set<Note> createNotesForClosedStations() {
        Set<Note> messages = new HashSet<>();
        config.getClosedStations().
                forEach(stationName -> messages.add(new Note(ClosedStation, format("%s is currently closed. %s", stationName, website))));
        return messages;
    }

    private <T extends CallsAtPlatforms> List<Note> liveNotesForJourneys(Set<T> journeys, TramServiceDate queryDate) {
        // Map: Note -> Location
        Map<Note,String> messageMap = new HashedMap<>();

        // find all the platforms involved in a journey, so board, depart and changes
        journeys.forEach(journey -> {
            // add messages for those platforms
            journey.getCallingPlatformIds().forEach(platform -> addRelevantNote(messageMap, platform, queryDate,
                    journey.getQueryTime()));
            });

        return createMessageList(messageMap);
    }

    private void addRelevantNote(Map<Note, String> messageMap, HasId platform, TramServiceDate queryDate, TramTime queryTime) {
        Optional<StationDepartureInfo> maybe = liveDataRepository.departuresFor(platform, queryDate, queryTime);
        if (maybe.isEmpty()) {
            return;
        }
        StationDepartureInfo info = maybe.get();
        LocalDateTime lastUpdate = info.getLastUpdate();
        if (!lastUpdate.toLocalDate().isEqual(queryDate.getDate())) {
            // message is not for journey time, perhaps journey is a future date or live data is stale
            return;
        }
        TramTime updateTime = TramTime.of(lastUpdate.toLocalTime());
        // 1 minutes here as time sync on live api has been out by 1 min
        if (!queryTime.between(updateTime.minusMinutes(1), updateTime.plusMinutes(MESSAGE_LIFETIME))) {
            return;
        }
        addRelevantNote(messageMap, info);
    }

    private List<Note> createMessageList(Map<Note, String> messageMap) {
        List<Note> messages = new ArrayList<>();
        messageMap.forEach((original,location) -> {
            if (location.isEmpty()) {
                messages.add(new Note(original.getNoteType(), format("'%s' - Metrolink", original.getText())));
            } else {
                messages.add(new Note(original.getNoteType(), format("'%s' - %s, Metrolink", original.getText(), location)));
            }
        });
        return messages;
    }

    private void addRelevantNote(Map<Note, String> messageMap, HasPlatformMessage info) {
        Note original = new Note(Live, info.getMessage());
        if (usefulNote(original)) {
            if (messageMap.containsKey(original)) {
                String existingLocation = messageMap.get(original);
                if (!existingLocation.equals(info.getLocation())) {
                    messageMap.put(original, ""); // must be shown at multiple locations, not specific
                }
            } else {
                messageMap.put(original, info.getLocation()); // initially specific to one location only
            }
        }
    }

    private boolean usefulNote(Note note) {
        return ! (note.getText().isEmpty() || EMPTY.equals(note.getText()));
    }
}
