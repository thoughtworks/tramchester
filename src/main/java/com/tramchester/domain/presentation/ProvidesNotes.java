package com.tramchester.domain.presentation;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import org.apache.commons.collections4.map.HashedMap;

import java.util.*;

import static java.lang.String.format;

public class ProvidesNotes {
    private final TramchesterConfig config;

    private static final String EMPTY = "<no message>";

    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";

    public static String weekend = "At the weekend your journey may be affected by improvement works." + website;

    public static String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;

    public ProvidesNotes(TramchesterConfig config) {
        this.config = config;
    }

    public List<String> createNotesFor(TramServiceDate queryDate, SortedSet<JourneyDTO> decoratedJourneys) {
        List<String> notes = new LinkedList<>();

        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }

        if (queryDate.isChristmasPeriod()) {
            notes.add(christmas);
        }

        notes.addAll(createNotesFor(config.getClosedStations()));

        notes.addAll(createNotesFor(decoratedJourneys));

        return notes;
    }

    private Set<String>  createNotesFor(List<String> closedStations) {
        Set<String> messages = new HashSet<>();
        closedStations.forEach(stationName -> {
            messages.add(format("%s is currently closed. %s", stationName, website));
        } );
        return messages;
    }

    private Set<String> createNotesFor(SortedSet<JourneyDTO> decoratedJourneys) {
        // Map: Message -> Location
        Map<String,String> messageMap = new HashedMap<>();

        decoratedJourneys.stream().forEach(journeyDTO -> journeyDTO.getStages().stream().
                filter(stageDTO -> stageDTO.getMode().equals(TransportMode.Tram)).
                forEach(tramStage -> addRelevantMessage(messageMap, tramStage))
        );

        return createMessageList(messageMap);
    }

    private Set<String> createMessageList(Map<String, String> messageMap) {
        Set<String> messages = new HashSet<>();
        messageMap.forEach((rawMessage,location) -> {
            if (location.isEmpty()) {
                messages.add(format("'%s' - Metrolink", rawMessage));
            } else {
                messages.add(format("'%s' - %s, Metrolink", rawMessage, location));
            }
        });

        return messages;
    }

    private void addRelevantMessage(Map<String,String> messageMap, StageDTO tramStage) {
        StationDepartureInfo info = tramStage.getPlatform().getStationDepartureInfo();
        if (info==null) {
            return;
        }

        String rawMessage = info.getMessage();
        if (usefulMessage(rawMessage)) {
            if (messageMap.containsKey(rawMessage)) {
                String existingLocation = messageMap.get(rawMessage);
                if (!existingLocation.equals(info.getLocation())) {
                    messageMap.put(rawMessage, ""); // must be shown at multiple locations, not specific
                }
            } else {
                messageMap.put(rawMessage, info.getLocation()); // initially specific to one location only
            }
        }
    }

    private boolean usefulMessage(String rawMessage) {
        return ! (rawMessage.isEmpty() || EMPTY.equals(rawMessage));
    }
}
