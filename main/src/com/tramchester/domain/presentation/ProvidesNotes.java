package com.tramchester.domain.presentation;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.HasPlatformMessage;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.repository.LiveDataRepository;
import org.apache.commons.collections4.map.HashedMap;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    public List<String> createNotesForJourneys(TramServiceDate queryDate, Set<Journey> journeys) {
        List<String> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(liveNotesForJourneys(journeys, queryDate));
        return notes;
    }

    public List<String> createNotesForStations(TramServiceDate queryDate, List<Station> stations) {
        List<String> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(createNotesForStations(stations));
        return notes;
    }

    private List<String> createNotesForStations(List<Station> stations) {
        // Map: Message -> Location
        Map<String,String> messageMap = new HashedMap<>();
        stations.forEach(station -> {
            liveDataRepository.departuresFor(station).forEach(info -> addRelevantMessage(messageMap, info));
        });

        return createMessageList(messageMap);
    }

    private List<String> createNotesForADate(TramServiceDate queryDate) {
        ArrayList<String> notes = new ArrayList<>();
        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }
        if (queryDate.isChristmasPeriod()) {
            notes.add(christmas);
        }
        notes.addAll(createNotesForClosedStations());
        return notes;
    }

    private Set<String> createNotesForClosedStations() {
        Set<String> messages = new HashSet<>();
        config.getClosedStations().
                forEach(stationName -> messages.add(format("%s is currently closed. %s", stationName, website)));
        return messages;
    }

    private List<String> liveNotesForJourneys(Set<Journey> journeys, TramServiceDate queryDate) {
        // Map: Message -> Location
        Map<String,String> messageMap = new HashedMap<>();

        // find all the platforms involved in a journey, so board, depart and changes
        journeys.forEach(journey ->{
            List<Platform> platformsForJourney = journey.getStages().stream().
                    filter(stage -> stage.getMode().equals(TransportMode.Tram)).
                    map(stage -> (VehicleStage) stage).
                    map(VehicleStage::getBoardingPlatform).filter(Optional::isPresent).
                    map(maybe -> maybe.get()).
                    collect(Collectors.toList());
            // add messages for those platforms
            platformsForJourney.forEach(platform -> {
                addRelevantMessage(messageMap, platform, journey.getQueryTime(), queryDate); });
            });

        return createMessageList(messageMap);
    }

    private void addRelevantMessage(Map<String, String> messageMap, Platform platform, TramTime queryTime, TramServiceDate queryDate) {
        Optional<StationDepartureInfo> maybe = liveDataRepository.departuresFor(platform);
        if (!maybe.isPresent()) {
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
        addRelevantMessage(messageMap, info);

    }

    private List<String> createMessageList(Map<String, String> messageMap) {
        List<String> messages = new ArrayList<>();
        messageMap.forEach((rawMessage,location) -> {
            if (location.isEmpty()) {
                messages.add(format("'%s' - Metrolink", rawMessage));
            } else {
                messages.add(format("'%s' - %s, Metrolink", rawMessage, location));
            }
        });
        return messages;
    }

    private void addRelevantMessage(Map<String, String> messageMap, HasPlatformMessage info) {
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
